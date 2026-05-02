package com.ptit.aia.dto;

import com.ptit.aia.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record IncomingMessage(
        @NotBlank String messageId,
        @NotNull Platform platform,
        @NotBlank String groupId,
        String groupName,
        @NotBlank String senderId,
        String senderName,
        @NotBlank String text,
        List<String> mentionedUserIds,
        OffsetDateTime receivedAt
) {}
