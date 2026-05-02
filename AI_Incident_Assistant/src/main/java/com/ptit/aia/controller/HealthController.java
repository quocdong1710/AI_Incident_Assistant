package com.ptit.aia.controller;

import com.ptit.aia.service.JiraService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JiraService jiraService;

    public HealthController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "jira", jiraService.healthCheck());
    }
}
