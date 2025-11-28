package com.lumichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conversation_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Builder.Default
    private Integer unreadCount = 0;

    @Builder.Default
    private Boolean isMuted = false;

    @Builder.Default
    private Boolean isPinned = false;

    @Builder.Default
    private Boolean isDeleted = false;

    @Column(columnDefinition = "text")
    private String draft;

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String atMsgIds = "[]";

    private Long lastReadMsgId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
