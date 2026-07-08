package com.messenger.api.messaging.dto;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
    String id,
    String type,
    String name,
    List<UserSummaryResponse> participants,
    String avatarUrl,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean isOnline
) {
}
