package com.ptit.aia.service;

import com.ptit.aia.config.AiaProperties;
import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.IncidentStatus;
import com.ptit.aia.domain.SlaBreachLog;
import com.ptit.aia.repository.IncidentRepository;
import com.ptit.aia.repository.SlaBreachLogRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SlaService {
    private static final Logger log = LoggerFactory.getLogger(SlaService.class);
    private final AiaProperties properties;
    private final IncidentRepository incidentRepository;
    private final SlaBreachLogRepository breachLogRepository;

    public SlaService(AiaProperties properties, IncidentRepository incidentRepository, SlaBreachLogRepository breachLogRepository) {
        this.properties = properties;
        this.incidentRepository = incidentRepository;
        this.breachLogRepository = breachLogRepository;
    }

    public OffsetDateTime calculateDeadline(Incident incident) {
        int minutes = properties.getSlaMinutes().getOrDefault(incident.getSeverity().name(), 240);
        return incident.getFirstReportedAt().plusMinutes(minutes);
    }

    @Scheduled(fixedDelay = 60000)
    public void monitorSla() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Incident> open = incidentRepository.findByStatusIn(List.of(IncidentStatus.Open, IncidentStatus.InProgress, IncidentStatus.Reopened));
        for (Incident incident : open) {
            if (incident.getFirstResponseAt() != null || incident.getSlaDeadlineAt() == null) continue;
            warnAtEightyPercent(now, incident);
            breachIfNeeded(now, incident);
        }
    }

    private void warnAtEightyPercent(OffsetDateTime now, Incident incident) {
        if (incident.getSlaWarningSentAt() != null) return;
        long totalMinutes = Duration.between(incident.getFirstReportedAt(), incident.getSlaDeadlineAt()).toMinutes();
        OffsetDateTime warningAt = incident.getFirstReportedAt().plusMinutes(Math.round(totalMinutes * 0.8));
        if (now.isAfter(warningAt)) {
            incident.setSlaWarningSentAt(now);
            incidentRepository.save(incident);
            log.warn("[SLA] {} ({}) còn {} phút phản hồi", incident.getJiraIssueKey(), incident.getSeverity(), Duration.between(now, incident.getSlaDeadlineAt()).toMinutes());
        }
    }

    private void breachIfNeeded(OffsetDateTime now, Incident incident) {
        if (!incident.isSlaBreached() && now.isAfter(incident.getSlaDeadlineAt())) {
            incident.setSlaBreached(true);
            incident.setEscalationLevel(1);
            incidentRepository.save(incident);
            SlaBreachLog logEntry = new SlaBreachLog();
            logEntry.setIncidentId(incident.getIncidentId());
            logEntry.setJiraIssueKey(incident.getJiraIssueKey());
            logEntry.setSeverity(incident.getSeverity());
            logEntry.setSlaDurationMinutes(properties.getSlaMinutes().getOrDefault(incident.getSeverity().name(), 240));
            logEntry.setBreachAt(now);
            logEntry.setFirstResponseAt(incident.getFirstResponseAt());
            logEntry.setDelayMinutes(Duration.between(incident.getSlaDeadlineAt(), now).toMinutes());
            logEntry.setEscalationLevel(1);
            breachLogRepository.save(logEntry);
            log.warn("[SLA BREACH] {} breached SLA", incident.getJiraIssueKey());
        }
    }
}
