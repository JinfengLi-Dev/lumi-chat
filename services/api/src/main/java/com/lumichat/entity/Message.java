package com.lumichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String msgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(length = 100)
    private String senderDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType msgType;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(length = 64)
    private String quoteMsgId;

    @Column(name = "at_user_ids", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] atUserIds;

    private LocalDateTime clientCreatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime serverCreatedAt;

    private LocalDateTime recalledAt;

    public enum MessageType {
        text, image, file, voice, video, location, user_card, group_card, system, recall
    }
}
