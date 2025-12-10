package com.lumichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "offline_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Column(length = 64)
    private String targetDeviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime expiredAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }

    public void markDelivered() {
        this.deliveredAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
