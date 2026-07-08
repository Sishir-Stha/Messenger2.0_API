package com.messenger.api.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    String conversationId,

    @NotBlank(message = "Message body is required")
    @Size(max = 4000, message = "Message body must be 4000 characters or less")
    String body
) {
}
