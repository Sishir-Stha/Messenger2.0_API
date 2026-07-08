package com.messenger.api.messaging.dto;

import java.time.Instant;

public record UserSummaryResponse(
    String id,
    String name,
    String username,
    String email,
    String avatarUrl,
    boolean isOnline,
    Instant lastActiveAt
) {
}
