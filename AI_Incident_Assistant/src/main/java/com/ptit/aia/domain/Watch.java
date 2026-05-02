package com.ptit.aia.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;

@Entity
public class Watch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String incidentId;
    private String jiraIssueKey;
    private String taggedUser;
    private OffsetDateTime watchStart;
    private OffsetDateTime expiresAt;
    @Enumerated(EnumType.STRING)
    private WatchStatus status = WatchStatus.waiting;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }
    public String getTaggedUser() { return taggedUser; }
    public void setTaggedUser(String taggedUser) { this.taggedUser = taggedUser; }
    public OffsetDateTime getWatchStart() { return watchStart; }
    public void setWatchStart(OffsetDateTime watchStart) { this.watchStart = watchStart; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public WatchStatus getStatus() { return status; }
    public void setStatus(WatchStatus status) { this.status = status; }
}
