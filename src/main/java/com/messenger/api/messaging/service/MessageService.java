package com.messenger.api.messaging.service;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.domain.Conversation;
import com.messenger.api.messaging.domain.ConversationMember;
import com.messenger.api.messaging.domain.Message;
import com.messenger.api.messaging.domain.MessageReceipt;
import com.messenger.api.messaging.domain.MessageStatus;
import com.messenger.api.messaging.dto.MessageResponse;
import com.messenger.api.messaging.dto.ReadReceiptResponse;
import com.messenger.api.messaging.repository.ConversationMemberRepository;
import com.messenger.api.messaging.repository.ConversationRepository;
import com.messenger.api.messaging.repository.MessageReceiptRepository;
import com.messenger.api.messaging.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;
    private final MessagingMapper mapper;

    public MessageService(
        ConversationService conversationService,
        ConversationRepository conversationRepository,
        ConversationMemberRepository memberRepository,
        MessageRepository messageRepository,
        MessageReceiptRepository receiptRepository,
        MessagingMapper mapper
    ) {
        this.conversationService = conversationService;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.receiptRepository = receiptRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String conversationId, Integer limit, AppUser user) {
        conversationService.requireConversationForUser(conversationId, user);
        int pageSize = Math.min(Math.max(limit == null ? DEFAULT_LIMIT : limit, 1), MAX_LIMIT);
        return messageRepository
            .findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                conversationId,
                PageRequest.of(0, pageSize, Sort.by("createdAt").ascending())
            )
            .stream()
            .map(message -> mapper.toMessageResponse(message, user))
            .toList();
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, String body, AppUser sender) {
        Conversation conversation = conversationService.requireConversationForUser(conversationId, sender);
        String trimmedBody = sanitizeBody(body);

        Message message = messageRepository.save(new Message(conversation, sender, trimmedBody));
        conversation.setLastMessage(message);
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        conversationService.getMembers(conversationId).stream()
            .map(ConversationMember::getUser)
            .filter(user -> !user.getId().equals(sender.getId()))
            .forEach(user -> receiptRepository.save(new MessageReceipt(message, user, MessageStatus.DELIVERED)));

        return mapper.toMessageResponse(message, sender);
    }

    @Transactional
    public MessageResponse forwardMessage(String sourceMessageId, String targetConversationId, AppUser sender) {
        Message sourceMessage = messageRepository.findById(sourceMessageId)
            .filter(message -> message.getDeletedAt() == null)
            .orElseThrow(() -> new MessagingNotFoundException("Source message was not found."));
        conversationService.requireConversationForUser(sourceMessage.getConversation().getId(), sender);

        Conversation targetConversation = conversationService.requireConversationForUser(targetConversationId, sender);
        Message forwardedMessage = new Message(targetConversation, sender, sourceMessage.getBody());
        forwardedMessage.setForwardedFromMessage(sourceMessage);
        Message savedMessage = messageRepository.save(forwardedMessage);

        targetConversation.setLastMessage(savedMessage);
        targetConversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversationRepository.save(targetConversation);

        conversationService.getMembers(targetConversationId).stream()
            .map(ConversationMember::getUser)
            .filter(user -> !user.getId().equals(sender.getId()))
            .forEach(user -> receiptRepository.save(new MessageReceipt(savedMessage, user, MessageStatus.DELIVERED)));

        return mapper.toMessageResponse(savedMessage, sender);
    }

    @Transactional
    public ReadReceiptResponse markAsRead(String conversationId, String messageId, AppUser user) {
        conversationService.requireConversationForUser(conversationId, user);
        Instant readAt = Instant.now();

        ConversationMember member = memberRepository
            .findByConversationIdAndUserIdAndDeletedAtIsNull(conversationId, user.getId())
            .orElseThrow(() -> new MessagingAccessDeniedException("User is not a conversation member."));

        Message lastReadMessage = resolveLastReadMessage(conversationId, messageId);
        if (lastReadMessage == null) {
            member.setLastReadAt(readAt);
            memberRepository.save(member);
            return new ReadReceiptResponse(conversationId, null, readAt);
        }

        member.setLastReadMessage(lastReadMessage);
        member.setLastReadAt(readAt);
        memberRepository.save(member);

        messageRepository.findMessagesToMarkRead(conversationId, user.getId(), lastReadMessage.getCreatedAt())
            .forEach(message -> {
                MessageReceipt receipt = receiptRepository.findByMessageIdAndUserId(message.getId(), user.getId())
                    .orElseGet(() -> new MessageReceipt(message, user, MessageStatus.READ));
                receipt.markRead();
                receiptRepository.save(receipt);
            });

        return new ReadReceiptResponse(conversationId, lastReadMessage.getId(), readAt);
    }

    private Message resolveLastReadMessage(String conversationId, String messageId) {
        if (messageId != null && !messageId.isBlank()) {
            Message message = messageRepository.findById(messageId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new MessagingNotFoundException("Message was not found."));
            if (!message.getConversation().getId().equals(conversationId)) {
                throw new IllegalArgumentException("Message does not belong to the conversation.");
            }
            return message;
        }
        return messageRepository.findFirstByConversationIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(conversationId)
            .orElse(null);
    }

    private String sanitizeBody(String body) {
        String trimmedBody = body == null ? "" : body.trim();
        if (trimmedBody.isBlank()) {
            throw new IllegalArgumentException("Message body is required.");
        }
        if (trimmedBody.length() > 4000) {
            throw new IllegalArgumentException("Message body must be 4000 characters or less.");
        }
        return trimmedBody;
    }
}
