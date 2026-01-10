package com.lumichat.service;

import com.lumichat.dto.request.QuickReplyRequest;
import com.lumichat.dto.response.QuickReplyResponse;
import com.lumichat.entity.QuickReply;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.QuickReplyRepository;
import com.lumichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuickReplyService {

    private static final int MAX_QUICK_REPLIES = 20;

    private final QuickReplyRepository quickReplyRepository;
    private final UserRepository userRepository;

    public List<QuickReplyResponse> getQuickReplies(Long userId) {
        return quickReplyRepository.findByUserIdOrderBySortOrderAsc(userId)
                .stream()
                .map(QuickReplyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuickReplyResponse createQuickReply(Long userId, QuickReplyRequest request) {
        int count = quickReplyRepository.countByUserId(userId);
        if (count >= MAX_QUICK_REPLIES) {
            throw new BadRequestException("Maximum number of quick replies (" + MAX_QUICK_REPLIES + ") reached");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        int maxSortOrder = quickReplyRepository.findMaxSortOrderByUserId(userId);

        QuickReply quickReply = QuickReply.builder()
                .user(user)
                .content(request.getContent())
                .sortOrder(maxSortOrder + 1)
                .build();

        quickReply = quickReplyRepository.save(quickReply);
        log.info("Created quick reply {} for user {}", quickReply.getId(), userId);

        return QuickReplyResponse.from(quickReply);
    }

    @Transactional
    public QuickReplyResponse updateQuickReply(Long id, Long userId, QuickReplyRequest request) {
        QuickReply quickReply = quickReplyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Quick reply not found"));

        quickReply.setContent(request.getContent());
        quickReply = quickReplyRepository.save(quickReply);
        log.info("Updated quick reply {} for user {}", id, userId);

        return QuickReplyResponse.from(quickReply);
    }

    @Transactional
    public void deleteQuickReply(Long id, Long userId) {
        int deleted = quickReplyRepository.deleteByIdAndUserId(id, userId);
        if (deleted == 0) {
            throw new NotFoundException("Quick reply not found");
        }
        log.info("Deleted quick reply {} for user {}", id, userId);
    }

    @Transactional
    public List<QuickReplyResponse> reorderQuickReplies(Long userId, List<Long> ids) {
        List<QuickReply> quickReplies = quickReplyRepository.findByUserIdOrderBySortOrderAsc(userId);

        // Verify all IDs belong to the user
        if (quickReplies.size() != ids.size()) {
            throw new BadRequestException("Invalid IDs list: count mismatch");
        }

        for (QuickReply qr : quickReplies) {
            if (!ids.contains(qr.getId())) {
                throw new BadRequestException("Invalid IDs list: missing ID " + qr.getId());
            }
        }

        // Update sort orders
        for (int i = 0; i < ids.size(); i++) {
            Long targetId = ids.get(i);
            for (QuickReply qr : quickReplies) {
                if (qr.getId().equals(targetId)) {
                    qr.setSortOrder(i);
                    break;
                }
            }
        }

        quickReplyRepository.saveAll(quickReplies);
        log.info("Reordered quick replies for user {}", userId);

        return quickReplies.stream()
                .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                .map(QuickReplyResponse::from)
                .collect(Collectors.toList());
    }
}
