package com.messenger.api.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "message_receipts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"})
)
public class MessageReceipt {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected MessageReceipt() {
    }

    public MessageReceipt(Message message, AppUser user, MessageStatus status) {
        this.message = message;
        this.user = user;
        this.status = status;
    }

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (status == MessageStatus.DELIVERED && deliveredAt == null) {
            deliveredAt = Instant.now();
        }
        if (status == MessageStatus.READ && readAt == null) {
            Instant now = Instant.now();
            deliveredAt = now;
            readAt = now;
        }
    }

    public Message getMessage() {
        return message;
    }

    public AppUser getUser() {
        return user;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void markRead() {
        status = MessageStatus.READ;
        Instant now = Instant.now();
        if (deliveredAt == null) {
            deliveredAt = now;
        }
        readAt = now;
    }
}
