package com.messenger.api.messaging.service;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.domain.Conversation;
import com.messenger.api.messaging.domain.ConversationMember;
import com.messenger.api.messaging.domain.ConversationType;
import com.messenger.api.messaging.domain.MemberRole;
import com.messenger.api.messaging.dto.ConversationResponse;
import com.messenger.api.messaging.dto.CreateConversationRequest;
import com.messenger.api.messaging.repository.AppUserRepository;
import com.messenger.api.messaging.repository.ConversationMemberRepository;
import com.messenger.api.messaging.repository.ConversationRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final AppUserRepository userRepository;
    private final MessagingMapper mapper;

    public ConversationService(
        ConversationRepository conversationRepository,
        ConversationMemberRepository memberRepository,
        AppUserRepository userRepository,
        MessagingMapper mapper
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(AppUser user) {
        return conversationRepository.findForUser(user.getId()).stream()
            .map(conversation -> mapper.toConversationResponse(conversation, user))
            .toList();
    }

    @Transactional
    public ConversationResponse createOrOpenConversation(CreateConversationRequest request, AppUser currentUser) {
        ConversationType type = parseType(request.type());
        Set<String> participantIds = new LinkedHashSet<>(request.participantIds());
        participantIds.add(currentUser.getId());

        if (type == ConversationType.DIRECT && participantIds.size() != 2) {
            throw new IllegalArgumentException("Direct conversations require exactly one other participant.");
        }

        List<AppUser> participants = participantIds.stream()
            .map(this::requireUser)
            .toList();

        if (type == ConversationType.DIRECT) {
            String otherUserId = participants.stream()
                .map(AppUser::getId)
                .filter(id -> !id.equals(currentUser.getId()))
                .findFirst()
                .orElseThrow();
            List<Conversation> existing = conversationRepository.findDirectBetween(
                ConversationType.DIRECT,
                currentUser.getId(),
                otherUserId
            );
            if (!existing.isEmpty()) {
                return mapper.toConversationResponse(existing.getFirst(), currentUser);
            }
        }

        String name = type == ConversationType.GROUP && StringUtils.hasText(request.name())
            ? request.name().trim()
            : null;
        Conversation conversation = conversationRepository.save(new Conversation(type, name, null, currentUser));
        participants.forEach(user -> memberRepository.save(new ConversationMember(conversation, user, MemberRole.MEMBER)));
        return mapper.toConversationResponse(conversation, currentUser);
    }

    @Transactional(readOnly = true)
    public Conversation requireConversationForUser(String conversationId, AppUser user) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .filter(item -> item.getDeletedAt() == null)
            .orElseThrow(() -> new MessagingNotFoundException("Conversation was not found."));

        if (!memberRepository.existsByConversationIdAndUserIdAndDeletedAtIsNull(conversationId, user.getId())) {
            throw new MessagingAccessDeniedException("User is not a conversation member.");
        }

        return conversation;
    }

    @Transactional(readOnly = true)
    public List<ConversationMember> getMembers(String conversationId) {
        return memberRepository.findByConversationIdAndDeletedAtIsNullOrderByJoinedAtAsc(conversationId);
    }

    private ConversationType parseType(String value) {
        try {
            return ConversationType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported conversation type.");
        }
    }

    private AppUser requireUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new MessagingNotFoundException("User was not found: " + userId));
    }
}
