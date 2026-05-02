package com.ptit.aia.dto;

public record ProcessResult(
        String status,
        String message,
        String incidentId,
        String jiraIssueKey,
        String jiraIssueUrl,
        boolean duplicate
) {}
