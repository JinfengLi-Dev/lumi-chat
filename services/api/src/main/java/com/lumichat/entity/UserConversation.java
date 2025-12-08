package com.lumichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

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
    private Boolean isHidden = false;

    @Column(columnDefinition = "text")
    private String draft;

    @Column(name = "at_msg_ids", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] atMsgIds;

    private Long lastReadMsgId;

    // Timestamp when user cleared messages; only show messages after this time
    private LocalDateTime clearedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
