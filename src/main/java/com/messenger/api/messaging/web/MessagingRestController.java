package com.messenger.api.messaging.web;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.dto.ConversationResponse;
import com.messenger.api.messaging.dto.CreateConversationRequest;
import com.messenger.api.messaging.dto.ForwardMessageRequest;
import com.messenger.api.messaging.dto.LoginRequest;
import com.messenger.api.messaging.dto.MessageResponse;
import com.messenger.api.messaging.dto.ReadReceiptRequest;
import com.messenger.api.messaging.dto.ReadReceiptResponse;
import com.messenger.api.messaging.dto.SendMessageRequest;
import com.messenger.api.messaging.dto.UserSummaryResponse;
import com.messenger.api.messaging.service.ConversationService;
import com.messenger.api.messaging.service.CurrentUserService;
import com.messenger.api.messaging.service.MessageService;
import com.messenger.api.messaging.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging")
public class MessagingRestController {
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final ConversationService conversationService;
    private final MessageService messageService;

    public MessagingRestController(
        CurrentUserService currentUserService,
        UserService userService,
        ConversationService conversationService,
        MessageService messageService
    ) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping("/me")
    public UserSummaryResponse getMe(HttpServletRequest request) {
        return userService.getCurrentUser(currentUser(request));
    }

    @PostMapping("/auth/login")
    public UserSummaryResponse login(@Valid @RequestBody LoginRequest body) {
        return userService.login(body.email(), body.password());
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getConversations(HttpServletRequest request) {
        return conversationService.getConversations(currentUser(request));
    }

    @PostMapping("/conversations")
    public ConversationResponse createConversation(
        @Valid @RequestBody CreateConversationRequest body,
        HttpServletRequest request
    ) {
        return conversationService.createOrOpenConversation(body, currentUser(request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(
        @PathVariable String conversationId,
        @RequestParam(required = false) Integer limit,
        HttpServletRequest request
    ) {
        return messageService.getMessages(conversationId, limit, currentUser(request));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendMessage(
        @PathVariable String conversationId,
        @Valid @RequestBody SendMessageRequest body,
        HttpServletRequest request
    ) {
        return messageService.sendMessage(conversationId, body.body(), currentUser(request));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ReadReceiptResponse markAsRead(
        @PathVariable String conversationId,
        @RequestBody(required = false) ReadReceiptRequest body,
        HttpServletRequest request
    ) {
        String messageId = body == null ? null : body.messageId();
        return messageService.markAsRead(conversationId, messageId, currentUser(request));
    }

    @GetMapping("/users/search")
    public List<UserSummaryResponse> searchUsers(@RequestParam(defaultValue = "") String q, HttpServletRequest request) {
        return userService.searchUsers(q, currentUser(request));
    }

    @PostMapping("/messages/{messageId}/forward")
    public List<MessageResponse> forwardMessage(
        @PathVariable String messageId,
        @Valid @RequestBody ForwardMessageRequest body,
        HttpServletRequest request
    ) {
        AppUser user = currentUser(request);
        return body.conversationIds().stream()
            .map(conversationId -> messageService.forwardMessage(messageId, conversationId, user))
            .toList();
    }

    private AppUser currentUser(HttpServletRequest request) {
        return currentUserService.getCurrentUser(request);
    }
}
