package com.teachquest.service;

import com.teachquest.model.ResourceFlag;
import com.teachquest.model.StudyResource;
import com.teachquest.model.User;
import com.teachquest.repository.ResourceFlagRepository;
import com.teachquest.repository.ResourcePointEventRepository;
import com.teachquest.repository.ResourceUpvoteRepository;
import com.teachquest.repository.StudyResourceRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResourceEngagementService {
    private static final Set<String> FLAG_REASONS = Set.of("OUTDATED", "INCORRECT", "DUPLICATE");

    private final ResourceFlagRepository flagRepository;
    private final StudyResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ResourcePointEventRepository eventRepository;
    private final ResourceUpvoteRepository upvoteRepository;

    public ResourceEngagementService(ResourceFlagRepository flagRepository,
                                     StudyResourceRepository resourceRepository,
                                     UserRepository userRepository,
                                     ResourcePointEventRepository eventRepository,
                                     ResourceUpvoteRepository upvoteRepository) {
        this.flagRepository = flagRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.upvoteRepository = upvoteRepository;
    }

    public List<Map<String, Object>> leaderboard(String period) {
        List<Object[]> rows = "month".equalsIgnoreCase(period) || "this-month".equalsIgnoreCase(period)
                ? eventRepository.leaderboardSince(LocalDateTime.now().minusMonths(1))
                : eventRepository.allTimeLeaderboard();
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            userRepository.findById(userId).ifPresent(user -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("rank", result.size() + 1);
                entry.put("userId", userId);
                entry.put("username", user.getUsername());
                entry.put("points", ((Number) row[1]).intValue());
                result.add(entry);
            });
            rank++;
        }
        return result;
    }

    public Map<String, Object> contributionSummary(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User account was not found."));
        long uploads = resourceRepository.countByUploaderId(userId);
        long upvotes = upvoteRepository.countReceivedByUploader(userId);
        int monthly = eventRepository.sumPointsSince(userId, LocalDateTime.now().minusMonths(1));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("thisMonth", monthly);
        result.put("allTime", user.getTotalPoints());
        result.put("uploads", uploads);
        result.put("upvotesReceived", upvotes);
        result.put("badges", badgeRules(uploads, upvotes));
        return result;
    }

    private List<Map<String, Object>> badgeRules(long uploads, long upvotes) {
        return List.of(
                badge("FIVE_UPLOADS", "Resource Contributor", "Upload 5 resources", uploads >= 5),
                badge("HUNDRED_UPVOTES", "Community Favorite", "Receive 100 upvotes", upvotes >= 100)
        );
    }

    private Map<String, Object> badge(String code, String label, String rule, boolean earned) {
        Map<String, Object> badge = new LinkedHashMap<>();
        badge.put("code", code);
        badge.put("label", label);
        badge.put("rule", rule);
        badge.put("earned", earned);
        return badge;
    }

    @Transactional
    public ResourceFlag createFlag(Long resourceId, Long reporterId, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim().toUpperCase();
        if (!FLAG_REASONS.contains(normalizedReason)) throw new IllegalArgumentException("Invalid flag reason.");
        if (!resourceRepository.existsById(resourceId)) throw new IllegalArgumentException("Resource was not found.");
        if (userRepository.findById(reporterId).isEmpty()) throw new IllegalArgumentException("Reporter account was not found.");
        if (flagRepository.existsByResourceIdAndReporterIdAndReason(resourceId, reporterId, normalizedReason)) {
            throw new IllegalArgumentException("You already submitted this report.");
        }
        ResourceFlag flag = new ResourceFlag();
        flag.setResourceId(resourceId);
        flag.setReporterId(reporterId);
        flag.setReason(normalizedReason);
        flag.setCreatedAt(LocalDateTime.now());
        return flagRepository.save(flag);
    }

    public List<Map<String, Object>> openFlags(Long adminId) {
        requireAdmin(adminId);
        return flagRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream().map(this::flagView).collect(Collectors.toList());
    }

    @Transactional
    public void dismissFlag(Long flagId, Long adminId) {
        requireAdmin(adminId);
        ResourceFlag flag = flagRepository.findById(flagId).orElseThrow(() -> new IllegalArgumentException("Flag was not found."));
        flag.setStatus("DISMISSED");
        flag.setReviewerId(adminId);
        flag.setReviewedAt(LocalDateTime.now());
        flagRepository.save(flag);
    }

    @Transactional
    public void removeResource(Long resourceId, Long adminId) {
        requireAdmin(adminId);
        StudyResource resource = resourceRepository.findById(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource was not found."));
        resource.setModerationStatus("REMOVED");
        resourceRepository.save(resource);
        flagRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .filter(flag -> flag.getResourceId().equals(resourceId))
                .forEach(flag -> {
                    flag.setStatus("REMOVED");
                    flag.setReviewerId(adminId);
                    flag.setReviewedAt(LocalDateTime.now());
                    flagRepository.save(flag);
                });
    }

    private void requireAdmin(Long adminId) {
        User admin = userRepository.findById(adminId).orElseThrow(() -> new IllegalArgumentException("Admin account was not found."));
        if (!"admin".equalsIgnoreCase(admin.getUserType())) throw new IllegalArgumentException("Administrator access is required.");
    }

    private Map<String, Object> flagView(ResourceFlag flag) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", flag.getId());
        view.put("resourceId", flag.getResourceId());
        view.put("reporterId", flag.getReporterId());
        view.put("reason", flag.getReason());
        view.put("status", flag.getStatus());
        view.put("createdAt", flag.getCreatedAt());
        resourceRepository.findById(flag.getResourceId()).ifPresent(resource -> {
            view.put("resourceTitle", resource.getCourseCode() + " - " + resource.getCategory());
            view.put("uploaderId", resource.getUploaderId());
        });
        return view;
    }
}
