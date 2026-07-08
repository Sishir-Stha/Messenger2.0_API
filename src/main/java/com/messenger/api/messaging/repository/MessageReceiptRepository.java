package com.messenger.api.messaging.repository;

import com.messenger.api.messaging.domain.MessageReceipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, String> {
    Optional<MessageReceipt> findByMessageIdAndUserId(String messageId, String userId);

    List<MessageReceipt> findByMessageId(String messageId);
}
