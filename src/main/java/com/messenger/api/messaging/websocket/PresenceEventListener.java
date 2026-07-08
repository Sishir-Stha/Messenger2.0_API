package com.messenger.api.messaging.websocket;

import com.messenger.api.messaging.service.UserService;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class PresenceEventListener {
    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);

    private final UserService userService;

    public PresenceEventListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            userService.markOnline(principal.getName());
            log.info("STOMP connected user={} session={}", principal.getName(), sessionId(event));
        } else {
            log.info("STOMP connected anonymous session={}", sessionId(event));
        }
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = event.getUser();
        log.info(
            "STOMP subscribed user={} session={} destination={}",
            principal == null ? "anonymous" : principal.getName(),
            accessor.getSessionId(),
            accessor.getDestination()
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            userService.markOffline(principal.getName());
            log.info(
                "STOMP disconnected user={} session={} status={}",
                principal.getName(),
                event.getSessionId(),
                event.getCloseStatus()
            );
        } else {
            log.info("STOMP disconnected anonymous session={} status={}", event.getSessionId(), event.getCloseStatus());
        }
    }

    private String sessionId(SessionConnectEvent event) {
        return StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
    }
}
