package com.messenger.api.messaging.service;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.domain.Conversation;
import com.messenger.api.messaging.domain.ConversationMember;
import com.messenger.api.messaging.domain.ConversationType;
import com.messenger.api.messaging.domain.Message;
import com.messenger.api.messaging.domain.MessageReceipt;
import com.messenger.api.messaging.domain.MessageStatus;
import com.messenger.api.messaging.dto.ConversationResponse;
import com.messenger.api.messaging.dto.MessageResponse;
import com.messenger.api.messaging.dto.UserSummaryResponse;
import com.messenger.api.messaging.repository.ConversationMemberRepository;
import com.messenger.api.messaging.repository.MessageReceiptRepository;
import com.messenger.api.messaging.repository.MessageRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MessagingMapper {
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;

    public MessagingMapper(
        ConversationMemberRepository memberRepository,
        MessageRepository messageRepository,
        MessageReceiptRepository receiptRepository
    ) {
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.receiptRepository = receiptRepository;
    }

    public UserSummaryResponse toUserSummary(AppUser user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.isOnline(),
            user.getLastActiveAt()
        );
    }

    public ConversationResponse toConversationResponse(Conversation conversation, AppUser viewer) {
        List<ConversationMember> members = memberRepository
            .findByConversationIdAndDeletedAtIsNullOrderByJoinedAtAsc(conversation.getId());
        List<UserSummaryResponse> participants = members.stream()
            .map(ConversationMember::getUser)
            .map(this::toUserSummary)
            .toList();

        AppUser otherDirectParticipant = members.stream()
            .map(ConversationMember::getUser)
            .filter(user -> !user.getId().equals(viewer.getId()))
            .findFirst()
            .orElse(null);

        String name = conversation.getType() == ConversationType.DIRECT && otherDirectParticipant != null
            ? otherDirectParticipant.getName()
            : conversation.getName();
        String avatarUrl = conversation.getType() == ConversationType.DIRECT && otherDirectParticipant != null
            ? otherDirectParticipant.getAvatarUrl()
            : conversation.getAvatarUrl();
        boolean online = conversation.getType() == ConversationType.DIRECT
            ? otherDirectParticipant != null && otherDirectParticipant.isOnline()
            : members.stream().map(ConversationMember::getUser)
                .anyMatch(user -> !user.getId().equals(viewer.getId()) && user.isOnline());

        ConversationMember viewerMember = members.stream()
            .filter(member -> member.getUser().getId().equals(viewer.getId()))
            .findFirst()
            .orElseThrow(() -> new MessagingAccessDeniedException("User is not a conversation member."));
        long unreadCount = messageRepository.countUnread(
            conversation.getId(),
            viewer.getId(),
            viewerMember.getLastReadAt() == null ? Instant.EPOCH : viewerMember.getLastReadAt()
        );

        Message lastMessage = conversation.getLastMessage();
        return new ConversationResponse(
            conversation.getId(),
            conversation.getType().name().toLowerCase(),
            name == null ? "Conversation" : name,
            participants,
            avatarUrl,
            lastMessage == null ? "" : lastMessage.getBody(),
            conversation.getLastMessageAt(),
            unreadCount,
            online
        );
    }

    public MessageResponse toMessageResponse(Message message, AppUser viewer) {
        return new MessageResponse(
            message.getId(),
            message.getConversation().getId(),
            message.getSender().getId(),
            message.getBody(),
            message.getCreatedAt(),
            resolveStatus(message, viewer).name().toLowerCase()
        );
    }

    private MessageStatus resolveStatus(Message message, AppUser viewer) {
        if (!message.getSender().getId().equals(viewer.getId())) {
            return MessageStatus.READ;
        }
        return receiptRepository.findByMessageId(message.getId()).stream()
            .map(MessageReceipt::getStatus)
            .max(Comparator.comparingInt(this::statusRank))
            .orElse(MessageStatus.SENT);
    }

    private int statusRank(MessageStatus status) {
        return switch (status) {
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
        };
    }
}
