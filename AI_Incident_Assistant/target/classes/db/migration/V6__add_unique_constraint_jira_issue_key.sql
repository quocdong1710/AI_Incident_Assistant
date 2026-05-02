-- Prevent duplicate incident creation when multiple Jira issues receive the same key
ALTER TABLE incident ADD CONSTRAINT uk_incident_jira_key UNIQUE (jira_issue_key);