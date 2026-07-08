package com.messenger.api.messaging.dto;

import java.time.Instant;

public record MessageStatusUpdate(
    String conversationId,
    String messageId,
    String status,
    String userId,
    Instant updatedAt
) {
}
