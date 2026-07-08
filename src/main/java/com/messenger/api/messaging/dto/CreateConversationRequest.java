package com.messenger.api.messaging.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateConversationRequest(
    @NotNull String type,
    String name,
    @NotEmpty List<String> participantIds
) {
}
