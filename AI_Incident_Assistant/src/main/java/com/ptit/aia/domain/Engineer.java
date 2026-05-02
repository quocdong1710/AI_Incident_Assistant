package com.ptit.aia.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Engineer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String displayName;
    private String skills;
    private int activeIncidentCount;
    private boolean online = true;
    private String accessComponents;
    private int recentAssignments;
    private String team;
    @Enumerated(EnumType.STRING)
    private EngineerStatus status = EngineerStatus.available;
    @jakarta.persistence.Column(name = "p1_count")
    private int p1Count = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public int getActiveIncidentCount() { return activeIncidentCount; }
    public void setActiveIncidentCount(int activeIncidentCount) { this.activeIncidentCount = activeIncidentCount; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public String getAccessComponents() { return accessComponents; }
    public void setAccessComponents(String accessComponents) { this.accessComponents = accessComponents; }
    public int getRecentAssignments() { return recentAssignments; }
    public void setRecentAssignments(int recentAssignments) { this.recentAssignments = recentAssignments; }
    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }
    public EngineerStatus getStatus() { return status; }
    public void setStatus(EngineerStatus status) { this.status = status; }
    public int getP1Count() { return p1Count; }
    public void setP1Count(int p1Count) { this.p1Count = p1Count; }
}
