package com.messenger.api.messaging.dto;

import java.time.Instant;

public record TypingEvent(
    String conversationId,
    String userId,
    boolean isTyping,
    Instant createdAt
) {
}
