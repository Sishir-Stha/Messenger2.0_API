package com.messenger.api.messaging.service;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CurrentUserService {
    public static final String DEFAULT_DEMO_USER_ID = "user-current";

    private final AppUserRepository userRepository;

    public CurrentUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser getCurrentUser(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && StringUtils.hasText(authentication.getName())) {
                userId = authentication.getName();
            }
        }
        return getCurrentUser(userId);
    }

    public AppUser getCurrentUser(Principal principal) {
        return getCurrentUser(principal == null ? null : principal.getName());
    }

    public AppUser getCurrentUser(String requestedUserId) {
        String userId = StringUtils.hasText(requestedUserId) ? requestedUserId : DEFAULT_DEMO_USER_ID;
        return userRepository.findById(userId)
            .orElseThrow(() -> new MessagingAccessDeniedException("Authenticated user was not found."));
    }
}
