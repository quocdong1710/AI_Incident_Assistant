package com.ptit.aia.controller;

import com.ptit.aia.domain.Platform;
import com.ptit.aia.dto.IncomingMessage;
import com.ptit.aia.dto.ProcessResult;
import com.ptit.aia.service.IncidentService;
import com.ptit.aia.service.WebhookSecurityService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private final IncidentService incidentService;
    private final WebhookSecurityService securityService;

    public WebhookController(IncidentService incidentService, WebhookSecurityService securityService) {
        this.incidentService = incidentService;
        this.securityService = securityService;
    }

    @PostMapping("/telegram")
    public ResponseEntity<ProcessResult> telegram(@RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String token,
                                                  @Valid @RequestBody IncomingMessage message) {
        if (!securityService.validateTelegram(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ProcessResult result = incidentService.process(new IncomingMessage(message.messageId(), Platform.telegram, message.groupId(), message.groupName(),
                message.senderId(), message.senderName(), message.text(), message.mentionedUserIds(), defaultTime(message.receivedAt())));
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping("/simulate")
    public ResponseEntity<ProcessResult> simulate(@Valid @RequestBody IncomingMessage message) {
        return ResponseEntity.ok(incidentService.process(message));
    }

    @PostMapping("/jira/status")
    public ResponseEntity<Map<String, String>> jiraStatusChanged(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("status", "received", "issue", payload.getOrDefault("issueKey", "unknown")));
    }

    private OffsetDateTime defaultTime(OffsetDateTime value) {
        return value == null ? OffsetDateTime.now() : value;
    }
}
