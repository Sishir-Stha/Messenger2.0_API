package com.messenger.api.messaging.repository;

import com.messenger.api.messaging.domain.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, String> {
    Optional<ConversationMember> findByConversationIdAndUserIdAndDeletedAtIsNull(String conversationId, String userId);

    boolean existsByConversationIdAndUserIdAndDeletedAtIsNull(String conversationId, String userId);

    List<ConversationMember> findByConversationIdAndDeletedAtIsNullOrderByJoinedAtAsc(String conversationId);

    List<ConversationMember> findByUserIdAndDeletedAtIsNull(String userId);
}
