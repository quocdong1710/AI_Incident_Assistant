package com.ptit.aia.service;

import com.ptit.aia.config.AiaProperties;
import com.ptit.aia.domain.Incident;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JiraService {
    private static final Logger log = LoggerFactory.getLogger(JiraService.class);
    private final AiaProperties properties;
    private final AtomicInteger sequence = new AtomicInteger(101);

    public JiraService(AiaProperties properties) {
        this.properties = properties;
    }

    public JiraIssue createIssue(Incident incident) {
        String key = properties.getJira().getProjectKey() + "-" + sequence.getAndIncrement();
        String url = properties.getJira().getBaseUrl() + "/browse/" + key;
        log.info("Creating Jira issue {} for incident {} with priority {}", key, incident.getIncidentId(), mapPriority(incident));
        return new JiraIssue(key, url);
    }

    public void addComment(String issueKey, String comment) {
        log.info("Adding Jira comment to {}: {}", issueKey, comment);
    }

    public void updatePriority(String issueKey, Incident incident) {
        log.info("Updating Jira priority for {} to {}", issueKey, mapPriority(incident));
    }

    public void assign(String issueKey, String username) {
        log.info("Assigning Jira issue {} to {}", issueKey, username);
    }

    public boolean healthCheck() {
        return properties.getJira().isMockEnabled() || properties.getJira().getBaseUrl() != null;
    }

    private String mapPriority(Incident incident) {
        return properties.getJira().getPriorityMapping().getOrDefault(incident.getSeverity().name(), incident.getSeverity().name());
    }

    public record JiraIssue(String key, String url) {}
}
