package com.messenger.api.messaging.dto;

import java.time.Instant;

public record MessageResponse(
    String id,
    String conversationId,
    String senderId,
    String body,
    Instant createdAt,
    String status
) {
}
