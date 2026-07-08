package com.messenger.api.messaging.dto;

public record ReadReceiptRequest(
    String conversationId,
    String messageId
) {
}
