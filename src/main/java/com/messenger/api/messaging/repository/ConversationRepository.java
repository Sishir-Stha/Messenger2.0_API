package com.messenger.api.messaging.repository;

import com.messenger.api.messaging.domain.Conversation;
import com.messenger.api.messaging.domain.ConversationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    @Query("""
        select distinct c from Conversation c
        join ConversationMember cm on cm.conversation = c
        where cm.user.id = :userId
          and cm.deletedAt is null
          and c.deletedAt is null
        order by c.lastMessageAt desc
        """)
    List<Conversation> findForUser(@Param("userId") String userId);

    @Query("""
        select c from Conversation c
        where c.type = :type
          and c.deletedAt is null
          and exists (
              select 1 from ConversationMember m1
              where m1.conversation = c and m1.user.id = :firstUserId and m1.deletedAt is null
          )
          and exists (
              select 1 from ConversationMember m2
              where m2.conversation = c and m2.user.id = :secondUserId and m2.deletedAt is null
          )
          and (
              select count(m3) from ConversationMember m3
              where m3.conversation = c and m3.deletedAt is null
          ) = 2
        """)
    List<Conversation> findDirectBetween(
        @Param("type") ConversationType type,
        @Param("firstUserId") String firstUserId,
        @Param("secondUserId") String secondUserId
    );
}
