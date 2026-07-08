package com.messenger.api.messaging.service;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.dto.UserSummaryResponse;
import com.messenger.api.messaging.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final AppUserRepository userRepository;
    private final MessagingMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, MessagingMapper mapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(AppUser user) {
        return mapper.toUserSummary(user);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse login(String email, String password) {
        String normalizedEmail = StringUtils.hasText(email) ? email.trim() : "";
        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new MessagingAccessDeniedException("Invalid email or password."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new MessagingAccessDeniedException("Invalid email or password.");
        }

        return mapper.toUserSummary(user);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> searchUsers(String query, AppUser currentUser) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
        return userRepository.search(normalizedQuery).stream()
            .filter(user -> !user.getId().equals(currentUser.getId()))
            .map(mapper::toUserSummary)
            .toList();
    }

    @Transactional
    public void markOnline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            user.setLastActiveAt(null);
        });
    }

    @Transactional
    public void markOffline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(false);
            user.setLastActiveAt(Instant.now());
        });
    }
}
