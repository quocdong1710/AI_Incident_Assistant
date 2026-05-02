package com.ptit.aia.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;

@Entity
public class SlaBreachLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String incidentId;
    private String jiraIssueKey;
    @Enumerated(EnumType.STRING)
    private Severity severity;
    private int slaDurationMinutes;
    private OffsetDateTime breachAt;
    private OffsetDateTime firstResponseAt;
    private long delayMinutes;
    private int escalationLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public int getSlaDurationMinutes() { return slaDurationMinutes; }
    public void setSlaDurationMinutes(int slaDurationMinutes) { this.slaDurationMinutes = slaDurationMinutes; }
    public OffsetDateTime getBreachAt() { return breachAt; }
    public void setBreachAt(OffsetDateTime breachAt) { this.breachAt = breachAt; }
    public OffsetDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(OffsetDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public long getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(long delayMinutes) { this.delayMinutes = delayMinutes; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
}
