package com.messenger.api.messaging.repository;

import com.messenger.api.messaging.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(String conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String conversationId);

    @Query("""
        select count(m) from Message m
        where m.conversation.id = :conversationId
          and m.sender.id <> :userId
          and m.deletedAt is null
          and m.createdAt > :lastReadAt
        """)
    long countUnread(
        @Param("conversationId") String conversationId,
        @Param("userId") String userId,
        @Param("lastReadAt") Instant lastReadAt
    );

    @Query("""
        select m from Message m
        where m.conversation.id = :conversationId
          and m.sender.id <> :userId
          and m.deletedAt is null
          and m.createdAt <= :readThrough
        order by m.createdAt asc
        """)
    List<Message> findMessagesToMarkRead(
        @Param("conversationId") String conversationId,
        @Param("userId") String userId,
        @Param("readThrough") Instant readThrough
    );
}
