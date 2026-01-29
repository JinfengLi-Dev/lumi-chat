package com.lumichat.service;

import com.lumichat.dto.response.ConversationResponse;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.Message;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.MessageRepository;
import com.lumichat.repository.UserConversationRepository;
import com.lumichat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.lumichat.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserConversationRepository userConversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConversationService conversationService;

    private User testUser;
    private User targetUser;
    private Conversation privateConversation;
    private UserConversation userConversation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .status(User.UserStatus.active)
                .build();

        targetUser = User.builder()
                .id(2L)
                .uid("LC87654321")
                .email("target@example.com")
                .nickname("TargetUser")
                .status(User.UserStatus.active)
                .build();

        privateConversation = Conversation.builder()
                .id(100L)
                .type(Conversation.ConversationType.private_chat)
                .participantIds(new Long[]{1L, 2L})
                .createdAt(LocalDateTime.now())
                .build();

        userConversation = UserConversation.builder()
                .id(1L)
                .user(testUser)
                .conversation(privateConversation)
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetUserConversations Tests")
    class GetUserConversationsTests {

        @Test
        @DisplayName("Should get user conversations successfully")
        void shouldGetUserConversationsSuccessfully() {
            // Given
            when(userConversationRepository.findAllByUserIdOrderByPinnedAndTime(1L))
                    .thenReturn(Arrays.asList(userConversation));
            // The optimized method uses batch loading
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(targetUser));
            when(messageRepository.findLatestMessagesForConversations(any()))
                    .thenReturn(Collections.emptyList());

            // When
            List<ConversationResponse> results = conversationService.getUserConversations(1L);

            // Then
            assertThat(results).hasSize(1);
            verify(userConversationRepository).findAllByUserIdOrderByPinnedAndTime(1L);
        }

        @Test
        @DisplayName("Should return empty list when no conversations")
        void shouldReturnEmptyListWhenNoConversations() {
            // Given
            when(userConversationRepository.findAllByUserIdOrderByPinnedAndTime(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<ConversationResponse> results = conversationService.getUserConversations(1L);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should handle exception when batch loading messages")
        void shouldHandleExceptionWhenBatchLoadingMessages() {
            // Given
            Conversation brokenConversation = Conversation.builder()
                    .id(999L)
                    .type(Conversation.ConversationType.private_chat)
                    .participantIds(new Long[]{1L, 2L})
                    .build();

            UserConversation brokenUc = UserConversation.builder()
                    .id(2L)
                    .user(testUser)
                    .conversation(brokenConversation)
                    .isHidden(false)
                    .build();

            when(userConversationRepository.findAllByUserIdOrderByPinnedAndTime(1L))
                    .thenReturn(Arrays.asList(brokenUc));
            // The optimized method uses findAllById which doesn't throw for missing users
            when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(messageRepository.findLatestMessagesForConversations(any()))
                    .thenThrow(new RuntimeException("Simulated error"));

            // When
            List<ConversationResponse> results = conversationService.getUserConversations(1L);

            // Then - the error is caught during batch loading, conversations still get returned
            // just without the last message data
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLastMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("GetConversation Tests")
    class GetConversationTests {

        @Test
        @DisplayName("Should get conversation successfully")
        void shouldGetConversationSuccessfully() {
            // Given
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(messageRepository.findByConversationIdAfterClearedAt(eq(100L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ConversationResponse result = conversationService.getConversation(1L, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowExceptionWhenConversationNotFound() {
            // Given
            when(userConversationRepository.findByUserIdAndConversationId(1L, 999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> conversationService.getConversation(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Conversation not found");
        }

        @Test
        @DisplayName("Should throw exception when conversation is hidden")
        void shouldThrowExceptionWhenConversationIsHidden() {
            // Given
            userConversation.setIsHidden(true);
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));

            // When/Then
            assertThatThrownBy(() -> conversationService.getConversation(1L, 100L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Conversation not found");
        }
    }

    @Nested
    @DisplayName("DeleteConversation Tests")
    class DeleteConversationTests {

        @Test
        @DisplayName("Should delete conversation (soft delete)")
        void shouldDeleteConversationSoftDelete() {
            // When
            conversationService.deleteConversation(1L, 100L);

            // Then
            verify(userConversationRepository).softDelete(1L, 100L);
        }
    }

    @Nested
    @DisplayName("MarkAsRead Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark conversation as read with messages")
        void shouldMarkConversationAsReadWithMessages() {
            // Given
            Message lastMessage = Message.builder()
                    .id(500L)
                    .msgId("msg-123")
                    .build();

            when(messageRepository.findByConversationIdOrderByServerCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(lastMessage)));

            // When
            conversationService.markAsRead(1L, 100L);

            // Then
            verify(userConversationRepository).markAsRead(1L, 100L, 500L);
        }

        @Test
        @DisplayName("Should mark conversation as read with no messages")
        void shouldMarkConversationAsReadWithNoMessages() {
            // Given
            when(messageRepository.findByConversationIdOrderByServerCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            conversationService.markAsRead(1L, 100L);

            // Then
            verify(userConversationRepository).markAsRead(1L, 100L, null);
        }
    }

    @Nested
    @DisplayName("ToggleMute Tests")
    class ToggleMuteTests {

        @Test
        @DisplayName("Should mute conversation")
        void shouldMuteConversation() {
            // When
            conversationService.toggleMute(1L, 100L, true);

            // Then
            verify(userConversationRepository).setMuted(1L, 100L, true);
        }

        @Test
        @DisplayName("Should unmute conversation")
        void shouldUnmuteConversation() {
            // When
            conversationService.toggleMute(1L, 100L, false);

            // Then
            verify(userConversationRepository).setMuted(1L, 100L, false);
        }
    }

    @Nested
    @DisplayName("TogglePin Tests")
    class TogglePinTests {

        @Test
        @DisplayName("Should pin conversation")
        void shouldPinConversation() {
            // When
            conversationService.togglePin(1L, 100L, true);

            // Then
            verify(userConversationRepository).setPinned(1L, 100L, true);
        }

        @Test
        @DisplayName("Should unpin conversation")
        void shouldUnpinConversation() {
            // When
            conversationService.togglePin(1L, 100L, false);

            // Then
            verify(userConversationRepository).setPinned(1L, 100L, false);
        }
    }

    @Nested
    @DisplayName("SaveDraft Tests")
    class SaveDraftTests {

        @Test
        @DisplayName("Should save draft")
        void shouldSaveDraft() {
            // When
            conversationService.saveDraft(1L, 100L, "Draft message");

            // Then
            verify(userConversationRepository).saveDraft(1L, 100L, "Draft message");
        }

        @Test
        @DisplayName("Should clear draft with null")
        void shouldClearDraftWithNull() {
            // When
            conversationService.saveDraft(1L, 100L, null);

            // Then
            verify(userConversationRepository).saveDraft(1L, 100L, null);
        }
    }

    @Nested
    @DisplayName("GetOrCreatePrivateConversation Tests")
    class GetOrCreatePrivateConversationTests {

        @Test
        @DisplayName("Should return existing conversation")
        void shouldReturnExistingConversation() {
            // Given
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.of(privateConversation));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(messageRepository.findByConversationIdAfterClearedAt(eq(100L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ConversationResponse result = conversationService.getOrCreatePrivateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(conversationRepository, never()).save(any(Conversation.class));
        }

        @Test
        @DisplayName("Should unhide existing hidden conversation")
        void shouldUnhideExistingHiddenConversation() {
            // Given
            userConversation.setIsHidden(true);
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.of(privateConversation));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(userConversationRepository.save(any(UserConversation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(messageRepository.findByConversationIdAfterClearedAt(eq(100L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ConversationResponse result = conversationService.getOrCreatePrivateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(userConversationRepository).save(userConversation);
            assertThat(userConversation.getIsHidden()).isFalse();
        }

        @Test
        @DisplayName("Should create new conversation when none exists")
        void shouldCreateNewConversationWhenNoneExists() {
            // Given
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
                Conversation c = inv.getArgument(0);
                c.setId(200L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(userConversationRepository.save(any(UserConversation.class))).thenAnswer(inv -> {
                UserConversation uc = inv.getArgument(0);
                uc.setId(10L);
                return uc;
            });
            when(messageRepository.findByConversationIdAfterClearedAt(eq(200L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ConversationResponse result = conversationService.getOrCreatePrivateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(conversationRepository).save(any(Conversation.class));
            verify(userConversationRepository, times(2)).save(any(UserConversation.class));
        }

        @Test
        @DisplayName("Should throw exception when target user not found")
        void shouldThrowExceptionWhenTargetUserNotFound() {
            // Given
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> conversationService.getOrCreatePrivateConversation(1L, 2L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Target user not found");
        }

        @Test
        @DisplayName("Should throw exception when current user not found")
        void shouldThrowExceptionWhenCurrentUserNotFound() {
            // Given
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> conversationService.getOrCreatePrivateConversation(1L, 2L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Current user not found");
        }

        @Test
        @DisplayName("Should not match group conversations")
        void shouldNotMatchGroupConversations() {
            // Given
            when(conversationRepository.findPrivateChat(1L, 2L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
                Conversation c = inv.getArgument(0);
                c.setId(200L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(userConversationRepository.save(any(UserConversation.class))).thenAnswer(inv -> {
                UserConversation uc = inv.getArgument(0);
                uc.setId(10L);
                return uc;
            });
            when(messageRepository.findByConversationIdAfterClearedAt(eq(200L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ConversationResponse result = conversationService.getOrCreatePrivateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(conversationRepository).save(any(Conversation.class)); // New conversation created
        }
    }

    @Nested
    @DisplayName("Stranger Conversation Tests")
    class StrangerConversationTests {

        @Test
        @DisplayName("Should build response for stranger conversation")
        void shouldBuildResponseForStrangerConversation() {
            // Given
            Conversation strangerConversation = Conversation.builder()
                    .id(400L)
                    .type(Conversation.ConversationType.stranger)
                    .participantIds(new Long[]{1L, 2L})
                    .createdAt(LocalDateTime.now())
                    .build();

            UserConversation strangerUc = UserConversation.builder()
                    .id(4L)
                    .user(testUser)
                    .conversation(strangerConversation)
                    .isHidden(false)
                    .build();

            when(userConversationRepository.findAllByUserIdOrderByPinnedAndTime(1L))
                    .thenReturn(Collections.singletonList(strangerUc));
            // The optimized method uses batch loading
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(targetUser));
            when(messageRepository.findLatestMessagesForConversations(any()))
                    .thenReturn(Collections.emptyList());

            // When
            List<ConversationResponse> results = conversationService.getUserConversations(1L);

            // Then
            assertThat(results).hasSize(1);
        }
    }
}
