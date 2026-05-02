package com.ptit.aia.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aia")
public class AiaProperties {
    private double confidenceThreshold = 0.70;
    private double uncertainThreshold = 0.40;
    private double similarityThreshold = 0.80;
    private double possibleDuplicateThreshold = 0.60;
    private boolean mentionOnlyMode;
    private Gemini gemini = new Gemini();
    private Telegram telegram = new Telegram();
    private Jira jira = new Jira();
    private Map<String, Integer> slaMinutes = new HashMap<>();
    private WorkingHours workingHours = new WorkingHours();

    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    public double getUncertainThreshold() { return uncertainThreshold; }
    public void setUncertainThreshold(double uncertainThreshold) { this.uncertainThreshold = uncertainThreshold; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public double getPossibleDuplicateThreshold() { return possibleDuplicateThreshold; }
    public void setPossibleDuplicateThreshold(double possibleDuplicateThreshold) { this.possibleDuplicateThreshold = possibleDuplicateThreshold; }
    public boolean isMentionOnlyMode() { return mentionOnlyMode; }
    public void setMentionOnlyMode(boolean mentionOnlyMode) { this.mentionOnlyMode = mentionOnlyMode; }
    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    public Telegram getTelegram() { return telegram; }
    public void setTelegram(Telegram telegram) { this.telegram = telegram; }
    public Jira getJira() { return jira; }
    public void setJira(Jira jira) { this.jira = jira; }
    public Map<String, Integer> getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Map<String, Integer> slaMinutes) { this.slaMinutes = slaMinutes; }
    public WorkingHours getWorkingHours() { return workingHours; }
    public void setWorkingHours(WorkingHours workingHours) { this.workingHours = workingHours; }

    public static class Gemini {
        private String apiKey;
        private boolean enabled = true;
        private String model = "gemini-2.0-flash";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Telegram {
        private String botSecretToken;
        public String getBotSecretToken() { return botSecretToken; }
        public void setBotSecretToken(String botSecretToken) { this.botSecretToken = botSecretToken; }
    }


    public static class Jira {
        private String baseUrl;
        private String projectKey;
        private String apiVersion;
        private String authMethod;
        private String username;
        private String apiToken;
        private boolean mockEnabled = true;
        private Map<String, String> priorityMapping = new HashMap<>();
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getProjectKey() { return projectKey; }
        public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getApiToken() { return apiToken; }
        public void setApiToken(String apiToken) { this.apiToken = apiToken; }
        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public Map<String, String> getPriorityMapping() { return priorityMapping; }
        public void setPriorityMapping(Map<String, String> priorityMapping) { this.priorityMapping = priorityMapping; }
    }

    public static class WorkingHours {
        private String timezone = "Asia/Ho_Chi_Minh";
        private String start = "08:00";
        private String end = "17:30";
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
