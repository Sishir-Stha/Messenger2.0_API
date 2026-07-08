package com.messenger.api.messaging.dto;

import java.time.Instant;

public record ReadReceiptResponse(
    String conversationId,
    String lastReadMessageId,
    Instant readAt
) {
}
