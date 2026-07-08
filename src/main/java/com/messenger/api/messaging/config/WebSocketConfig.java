package com.messenger.api.messaging.config;

import com.messenger.api.messaging.service.CurrentUserService;
import java.net.URLDecoder;
import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final String websocketEndpoint;
    private final String appPrefix;
    private final String allowedOriginPatterns;

    public WebSocketConfig(
        @Value("${messaging.websocket.endpoint}") String websocketEndpoint,
        @Value("${messaging.stomp.app-prefix}") String appPrefix,
        @Value("${messaging.cors.allowed-origin-patterns}") String allowedOriginPatterns
    ) {
        this.websocketEndpoint = websocketEndpoint;
        this.appPrefix = appPrefix;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes(appPrefix);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOriginPatterns.split(",");
        registry.addEndpoint(websocketEndpoint)
            .setAllowedOriginPatterns(origins)
            .setHandshakeHandler(new DemoPrincipalHandshakeHandler());
        registry.addEndpoint(websocketEndpoint)
            .setAllowedOriginPatterns(origins)
            .setHandshakeHandler(new DemoPrincipalHandshakeHandler())
            .withSockJS();
    }

    private static class DemoPrincipalHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
        ) {
            String userId = resolveUserId(request);
            if (!StringUtils.hasText(userId)) {
                userId = CurrentUserService.DEFAULT_DEMO_USER_ID;
            }
            String principalName = userId;
            log.info("WebSocket handshake resolved user={} uri={}", principalName, request.getURI().getPath());
            return () -> principalName;
        }

        private String resolveUserId(ServerHttpRequest request) {
            String headerUserId = request.getHeaders().getFirst("X-User-Id");
            if (StringUtils.hasText(headerUserId)) {
                return headerUserId;
            }

            String query = request.getURI().getRawQuery();
            if (!StringUtils.hasText(query)) {
                return null;
            }

            for (String parameter : query.split("&")) {
                String[] parts = parameter.split("=", 2);
                if (parts.length == 2 && "userId".equals(parts[0])) {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            return null;
        }
    }
}
