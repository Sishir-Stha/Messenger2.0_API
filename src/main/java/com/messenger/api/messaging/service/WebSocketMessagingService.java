package com.messenger.api.messaging.service;

import com.messenger.api.messaging.dto.MessageResponse;
import com.messenger.api.messaging.dto.MessageStatusUpdate;
import com.messenger.api.messaging.dto.ReadReceiptResponse;
import com.messenger.api.messaging.dto.TypingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketMessagingService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessagingService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final String roomTopicDestination;
    private final String userQueueDestination;

    public WebSocketMessagingService(
        SimpMessagingTemplate messagingTemplate,
        @Value("${messaging.stomp.room-topic-destination}") String roomTopicDestination,
        @Value("${messaging.stomp.user-queue-destination}") String userQueueDestination
    ) {
        this.messagingTemplate = messagingTemplate;
        this.roomTopicDestination = roomTopicDestination;
        this.userQueueDestination = userQueueDestination;
    }

    public void publishMessage(MessageResponse message) {
        String destination = roomTopicDestination + "/" + message.conversationId();
        messagingTemplate.convertAndSend(destination, message);
        log.info("STOMP message broadcast id={} destination={}", message.id(), destination);
    }

    public void sendUserMessage(String userId, MessageResponse message) {
        messagingTemplate.convertAndSendToUser(userId, userQueueDestination, message);
        log.info("STOMP user message broadcast id={} user={} destination={}", message.id(), userId, userQueueDestination);
    }

    public void publishTyping(TypingEvent event) {
        String destination = roomTopicDestination + "/" + event.conversationId() + "/typing";
        messagingTemplate.convertAndSend(destination, event);
        log.info("STOMP typing broadcast user={} destination={}", event.userId(), destination);
    }

    public void publishReadReceipt(ReadReceiptResponse receipt) {
        MessageStatusUpdate update = new MessageStatusUpdate(
            receipt.conversationId(),
            receipt.lastReadMessageId(),
            "read",
            null,
            receipt.readAt()
        );
        String destination = roomTopicDestination + "/" + receipt.conversationId() + "/read";
        messagingTemplate.convertAndSend(destination, update);
        log.info("STOMP read receipt broadcast conversation={} destination={}", receipt.conversationId(), destination);
    }
}
