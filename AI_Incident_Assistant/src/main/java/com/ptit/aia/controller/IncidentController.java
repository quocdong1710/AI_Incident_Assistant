package com.ptit.aia.controller;

import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.Severity;
import com.ptit.aia.repository.IncidentRepository;
import com.ptit.aia.repository.SlaBreachLogRepository;
import com.ptit.aia.service.ResourceAdvisorService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IncidentController {
    private final IncidentRepository incidentRepository;
    private final SlaBreachLogRepository breachLogRepository;
    private final ResourceAdvisorService advisorService;

    public IncidentController(IncidentRepository incidentRepository, SlaBreachLogRepository breachLogRepository, ResourceAdvisorService advisorService) {
        this.incidentRepository = incidentRepository;
        this.breachLogRepository = breachLogRepository;
        this.advisorService = advisorService;
    }

    @GetMapping("/incidents")
    public List<Incident> list() {
        return incidentRepository.findAll();
    }

    @GetMapping("/incidents/{jiraIssueKey}")
    public Incident status(@PathVariable String jiraIssueKey) {
        return incidentRepository.findByJiraIssueKey(jiraIssueKey).orElseThrow();
    }

    @PostMapping("/commands/severity")
    public Incident overrideSeverity(@RequestParam String issueKey, @RequestParam Severity severity) {
        Incident incident = incidentRepository.findByJiraIssueKey(issueKey).orElseThrow();
        incident.setSeverity(severity);
        incident.setUpdatedAt(OffsetDateTime.now());
        return incidentRepository.save(incident);
    }

    @GetMapping("/commands/suggest/{issueKey}")
    public List<ResourceAdvisorService.Suggestion> suggest(@PathVariable String issueKey) {
        return advisorService.suggest(issueKey);
    }

    @PostMapping("/commands/assign")
    public Map<String, String> assign(@RequestParam String issueKey, @RequestParam String username) {
        advisorService.assign(issueKey, username);
        return Map.of("status", "assigned", "issueKey", issueKey, "username", username);
    }

    @GetMapping("/commands/report/sla")
    public Map<String, Object> slaReport(@RequestParam(defaultValue = "week") String period) {
        OffsetDateTime since = period.equalsIgnoreCase("month") ? OffsetDateTime.now().minusDays(30) : OffsetDateTime.now().minusDays(7);
        var breaches = breachLogRepository.findByBreachAtAfter(since);
        return Map.of(
                "period", period,
                "totalIncidents", incidentRepository.count(),
                "breachCount", breaches.size(),
                "breaches", breaches
        );
    }

    @GetMapping("/kpi")
    public Map<String, Object> kpi() {
        long total = incidentRepository.count();
        long open = incidentRepository.findByStatusIn(List.of(com.ptit.aia.domain.IncidentStatus.Open, com.ptit.aia.domain.IncidentStatus.InProgress)).size();
        long p1Count = incidentRepository.findAll().stream().filter(i -> i.getSeverity() == Severity.P1).count();
        long duplicateAvoided = incidentRepository.findAll().stream().mapToLong(i -> i.getGroupsReported() - 1).sum();
        long slaAlerts = breachLogRepository.count();
        return Map.of(
                "totalIncidents", total,
                "openIncidents", open,
                "p1Count", p1Count,
                "duplicateAvoided", duplicateAvoided,
                "slaAlerts", slaAlerts
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return kpi();
    }
}
