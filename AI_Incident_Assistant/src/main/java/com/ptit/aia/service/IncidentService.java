package com.ptit.aia.service;

import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.IncidentSource;
import com.ptit.aia.domain.IncidentStatus;
import com.ptit.aia.domain.MessageLog;
import com.ptit.aia.dto.IncomingMessage;
import com.ptit.aia.dto.ProcessResult;
import com.ptit.aia.dto.TriageResult;
import com.ptit.aia.repository.IncidentRepository;
import com.ptit.aia.repository.IncidentSourceRepository;
import com.ptit.aia.repository.MessageLogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private final TriageService triageService;
    private final MatchingService matchingService;
    private final JiraService jiraService;
    private final SlaService slaService;
    private final IncidentRepository incidentRepository;
    private final IncidentSourceRepository sourceRepository;
    private final MessageLogRepository messageLogRepository;

    public IncidentService(TriageService triageService, MatchingService matchingService, JiraService jiraService, SlaService slaService,
                           IncidentRepository incidentRepository, IncidentSourceRepository sourceRepository, MessageLogRepository messageLogRepository) {
        this.triageService = triageService;
        this.matchingService = matchingService;
        this.jiraService = jiraService;
        this.slaService = slaService;
        this.incidentRepository = incidentRepository;
        this.sourceRepository = sourceRepository;
        this.messageLogRepository = messageLogRepository;
    }

    @Transactional
    public ProcessResult process(IncomingMessage incoming) {
        OffsetDateTime receivedAt = incoming.receivedAt() == null ? OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")) : incoming.receivedAt();
        if (sourceRepository.findByMessageIdAndChatIdAndPlatform(incoming.messageId(), incoming.groupId(), incoming.platform()).isPresent()) {
            return new ProcessResult("duplicate_message", "Tin nhắn đã được xử lý trước đó.", null, null, null, true);
        }
        TriageResult triage = triageService.analyze(incoming);
        saveMessageLog(incoming, triage, receivedAt);
        if (!triage.bugReport()) {
            String message = triage.uncertain()
                    ? "[AIA] Bot nhận thấy đây có thể là lỗi phần mềm. Bạn có muốn tạo ticket hỗ trợ không?"
                    : "Ignored non-incident message";
            return new ProcessResult(triage.uncertain() ? "needs_confirmation" : "ignored", message, null, null, null, false);
        }
        return matchingService.findDuplicate(triage)
                .map(match -> mergeDuplicate(match.incident(), incoming, triage, receivedAt))
                .orElseGet(() -> createIncident(incoming, triage, receivedAt));
    }

    private ProcessResult createIncident(IncomingMessage incoming, TriageResult triage, OffsetDateTime receivedAt) {
        Incident incident = new Incident();
        incident.setIncidentId("INC-" + receivedAt.toLocalDate().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        incident.setTitle(triage.title());
        incident.setDescription(triage.description());
        incident.setSeverity(triage.severity());
        incident.setStatus(IncidentStatus.Open);
        incident.setComponent(triage.component());
        incident.setEnvironment(triage.environment());
        incident.setSourceLanguage(triage.sourceLanguage());
        incident.setImpactScope(triage.impactScope());
        incident.setGroupsReported(1);
        incident.setPlatforms(incoming.platform().name());
        incident.setFirstReportedAt(receivedAt);
        incident.setConfidenceScore(triage.confidence());
        incident.setCreatedAt(OffsetDateTime.now());
        incident.setUpdatedAt(OffsetDateTime.now());
        incident.setSlaDeadlineAt(slaService.calculateDeadline(incident));
        JiraService.JiraIssue issue = jiraService.createIssue(incident);
        incident.setJiraIssueKey(issue.key());
        incident.setJiraIssueUrl(issue.url());
        incidentRepository.save(incident);
        saveSource(incident, incoming, receivedAt);
        return new ProcessResult("created", formatCreatedMessage(incident, triage), incident.getIncidentId(), incident.getJiraIssueKey(), incident.getJiraIssueUrl(), false);
    }

    private ProcessResult mergeDuplicate(Incident incident, IncomingMessage incoming, TriageResult triage, OffsetDateTime receivedAt) {
        incident.setGroupsReported(incident.getGroupsReported() + 1);
        incident.setMentionCount(incident.getMentionCount() + 1);
        if (!incident.getPlatforms().contains(incoming.platform().name())) {
            incident.setPlatforms(incident.getPlatforms() + "," + incoming.platform().name());
        }
        if (incident.getGroupsReported() >= 3) {
            incident.setSeverity(incident.getSeverity().escalate());
            jiraService.updatePriority(incident.getJiraIssueKey(), incident);
        }
        incident.setUpdatedAt(OffsetDateTime.now());
        incidentRepository.save(incident);
        saveSource(incident, incoming, receivedAt);
        jiraService.addComment(incident.getJiraIssueKey(), "Duplicate report from " + incoming.platform() + " group " + incoming.groupId() + ": " + triage.description());
        return new ProcessResult("merged", "[AIA] Sự cố này trùng với " + incident.getJiraIssueKey() + " đang xử lý.", incident.getIncidentId(), incident.getJiraIssueKey(), incident.getJiraIssueUrl(), true);
    }

    private void saveMessageLog(IncomingMessage incoming, TriageResult triage, OffsetDateTime receivedAt) {
        MessageLog log = new MessageLog();
        log.setMessageId(incoming.messageId());
        log.setPlatform(incoming.platform());
        log.setGroupId(incoming.groupId());
        log.setGroupName(incoming.groupName());
        log.setSenderId(incoming.senderId());
        log.setSenderName(incoming.senderName());
        log.setMessageText(incoming.text());
        log.setMentionedUserIds(incoming.mentionedUserIds() == null ? "" : String.join(",", incoming.mentionedUserIds()));
        log.setReceivedAt(receivedAt);
        log.setClassification(triage.bugReport() ? "bug" : triage.uncertain() ? "uncertain" : "non_bug");
        log.setConfidenceScore(triage.confidence());
        messageLogRepository.save(log);
    }

    private void saveSource(Incident incident, IncomingMessage incoming, OffsetDateTime receivedAt) {
        IncidentSource source = new IncidentSource();
        source.setIncidentPk(incident.getId());
        source.setIncidentId(incident.getIncidentId());
        source.setJiraIssueKey(incident.getJiraIssueKey());
        source.setMessageId(incoming.messageId());
        source.setChatId(incoming.groupId());
        source.setPlatform(incoming.platform());
        source.setSenderId(incoming.senderId());
        source.setSenderName(incoming.senderName());
        source.setReceivedAt(receivedAt);
        sourceRepository.save(source);
    }

    private String formatCreatedMessage(Incident incident, TriageResult triage) {
        String slaText = incident.getSeverity() == com.ptit.aia.domain.Severity.P1 ? "30 phút" 
                       : incident.getSeverity() == com.ptit.aia.domain.Severity.P2 ? "2 giờ" : "trong ngày";
        return new StringJoiner("\n")
                .add("[AIA] ✅ Đã ghi nhận sự cố mới")
                .add("")
                .add("Ticket: " + incident.getJiraIssueKey())
                .add("Title: " + incident.getTitle())
                .add("Severity: " + incident.getSeverity())
                .add("SLA: " + slaText)
                .add("Status: " + incident.getStatus())
                .toString();
    }
}
