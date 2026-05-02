package com.ptit.aia.service;

import com.ptit.aia.domain.Engineer;
import com.ptit.aia.domain.Incident;
import com.ptit.aia.repository.EngineerRepository;
import com.ptit.aia.repository.IncidentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ResourceAdvisorService {
    private final EngineerRepository engineerRepository;
    private final IncidentRepository incidentRepository;
    private final JiraService jiraService;

    public ResourceAdvisorService(EngineerRepository engineerRepository, IncidentRepository incidentRepository, JiraService jiraService) {
        this.engineerRepository = engineerRepository;
        this.incidentRepository = incidentRepository;
        this.jiraService = jiraService;
    }

    public List<Suggestion> suggest(String jiraIssueKey) {
        Incident incident = incidentRepository.findByJiraIssueKey(jiraIssueKey).orElseThrow();
        String component = incident.getComponent() == null ? "" : incident.getComponent().toLowerCase(Locale.ROOT);
        return engineerRepository.findAll().stream()
                .map(engineer -> new Suggestion(engineer.getUsername(), engineer.getDisplayName(), score(engineer, component), reason(engineer, component)))
                .sorted(Comparator.comparingInt(Suggestion::score).reversed())
                .limit(3)
                .toList();
    }

    public void assign(String jiraIssueKey, String username) {
        Incident incident = incidentRepository.findByJiraIssueKey(jiraIssueKey).orElseThrow();
        Engineer engineer = engineerRepository.findByUsername(username.replace("@", "")).orElseThrow();
        incident.setAssignedTo(engineer.getUsername());
        incidentRepository.save(incident);
        engineer.setActiveIncidentCount(engineer.getActiveIncidentCount() + 1);
        engineer.setRecentAssignments(engineer.getRecentAssignments() + 1);
        engineerRepository.save(engineer);
        jiraService.assign(jiraIssueKey, engineer.getUsername());
    }

    private int score(Engineer engineer, String component) {
        int score = 0;
        String skills = safe(engineer.getSkills()).toLowerCase(Locale.ROOT);
        String access = safe(engineer.getAccessComponents()).toLowerCase(Locale.ROOT);
        if (!component.isBlank() && (skills.contains(component) || access.contains(component))) score += 50;
        score += Math.max(0, 25 - engineer.getActiveIncidentCount() * 5);
        if (engineer.isOnline()) score += 15;
        score += Math.max(0, 10 - engineer.getRecentAssignments() * 2);
        return score;
    }

    private String reason(Engineer engineer, String component) {
        return "Skills: " + safe(engineer.getSkills()) + " | Workload: " + engineer.getActiveIncidentCount() + " | Online: " + engineer.isOnline();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record Suggestion(String username, String displayName, int score, String reason) {}
}
