package com.messenger.api.messaging.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ForwardMessageRequest(
    @NotEmpty List<String> conversationIds
) {
}
