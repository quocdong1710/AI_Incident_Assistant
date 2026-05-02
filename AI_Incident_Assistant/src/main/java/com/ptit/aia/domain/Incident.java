package com.ptit.aia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;

@Entity
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String incidentId;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(length = 4000)
    private String description;
    @Enumerated(EnumType.STRING)
    private Severity severity;
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;
    private String component;
    private String environment;
    private String sourceLanguage;
    private String impactScope;
    private String jiraIssueKey;
    private String jiraIssueUrl;
    private Integer groupsReported = 1;
    @Column(name = "mention_count")
    private Integer mentionCount = 1;
    private String platforms;
    private OffsetDateTime firstReportedAt;
    private OffsetDateTime slaDeadlineAt;
    private OffsetDateTime slaWarningSentAt;
    private OffsetDateTime firstResponseAt;
    private boolean slaBreached;
    private Integer escalationLevel = 0;
    private String assignedTo;
    private double confidenceScore;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public String getImpactScope() { return impactScope; }
    public void setImpactScope(String impactScope) { this.impactScope = impactScope; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }
    public String getJiraIssueUrl() { return jiraIssueUrl; }
    public void setJiraIssueUrl(String jiraIssueUrl) { this.jiraIssueUrl = jiraIssueUrl; }
    public Integer getGroupsReported() { return groupsReported; }
    public void setGroupsReported(Integer groupsReported) { this.groupsReported = groupsReported; }
    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public OffsetDateTime getFirstReportedAt() { return firstReportedAt; }
    public void setFirstReportedAt(OffsetDateTime firstReportedAt) { this.firstReportedAt = firstReportedAt; }
    public OffsetDateTime getSlaDeadlineAt() { return slaDeadlineAt; }
    public void setSlaDeadlineAt(OffsetDateTime slaDeadlineAt) { this.slaDeadlineAt = slaDeadlineAt; }
    public OffsetDateTime getSlaWarningSentAt() { return slaWarningSentAt; }
    public void setSlaWarningSentAt(OffsetDateTime slaWarningSentAt) { this.slaWarningSentAt = slaWarningSentAt; }
    public OffsetDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(OffsetDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getMentionCount() { return mentionCount; }
    public void setMentionCount(Integer mentionCount) { this.mentionCount = mentionCount; }

    @jakarta.persistence.Transient
    public SlaStatus getSlaStatus() {
        if (escalationLevel != null && escalationLevel > 0) return SlaStatus.Escalated;
        if (slaBreached) return SlaStatus.Overdue;
        if (slaWarningSentAt != null) return SlaStatus.Warning;
        return SlaStatus.On_Track;
    }
}
