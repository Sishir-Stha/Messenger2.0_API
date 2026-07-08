package com.messenger.api.messaging.websocket;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.dto.MessageResponse;
import com.messenger.api.messaging.dto.ReadReceiptRequest;
import com.messenger.api.messaging.dto.ReadReceiptResponse;
import com.messenger.api.messaging.dto.SendMessageRequest;
import com.messenger.api.messaging.dto.TypingEvent;
import com.messenger.api.messaging.service.ConversationService;
import com.messenger.api.messaging.service.CurrentUserService;
import com.messenger.api.messaging.service.MessageService;
import com.messenger.api.messaging.service.WebSocketMessagingService;
import java.security.Principal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@Controller
public class MessagingSocketController {
    private static final Logger log = LoggerFactory.getLogger(MessagingSocketController.class);

    private final CurrentUserService currentUserService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final WebSocketMessagingService socketMessagingService;

    public MessagingSocketController(
        CurrentUserService currentUserService,
        ConversationService conversationService,
        MessageService messageService,
        WebSocketMessagingService socketMessagingService
    ) {
        this.currentUserService = currentUserService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.socketMessagingService = socketMessagingService;
    }

    @MessageMapping("/messages.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        AppUser user = currentUserService.getCurrentUser(principal);
        log.info("STOMP message received user={} conversation={}", user.getId(), request.conversationId());
        MessageResponse response = messageService.sendMessage(request.conversationId(), request.body(), user);
        log.info("STOMP message persisted id={} conversation={} sender={}", response.id(), response.conversationId(), response.senderId());
        socketMessagingService.publishMessage(response);
    }

    @MessageMapping("/typing")
    public void typing(TypingEvent event, Principal principal) {
        AppUser user = currentUserService.getCurrentUser(principal);
        String conversationId = event.conversationId();
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("Conversation id is required.");
        }

        log.info("STOMP typing received user={} conversation={} isTyping={}", user.getId(), conversationId, event.isTyping());
        conversationService.requireConversationForUser(conversationId, user);
        log.debug("STOMP typing membership validated user={} conversation={}", user.getId(), conversationId);

        socketMessagingService.publishTyping(new TypingEvent(
            conversationId,
            user.getId(),
            event.isTyping(),
            Instant.now()
        ));
        log.debug("STOMP typing forwarded user={} conversation={} isTyping={}", user.getId(), conversationId, event.isTyping());
    }

    @MessageMapping("/messages.read")
    public void read(ReadReceiptRequest request, Principal principal) {
        AppUser user = currentUserService.getCurrentUser(principal);
        log.info("STOMP read receipt user={} conversation={} message={}", user.getId(), request.conversationId(), request.messageId());
        ReadReceiptResponse response = messageService.markAsRead(request.conversationId(), request.messageId(), user);
        socketMessagingService.publishReadReceipt(response);
    }

    @MessageExceptionHandler
    public void handleException(Exception exception, Principal principal) {
        log.warn(
            "STOMP message handling failed user={} error={}",
            principal == null ? "anonymous" : principal.getName(),
            exception.getMessage(),
            exception
        );
    }
}
