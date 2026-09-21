package com.teachquest.service;

import com.teachquest.model.StudyResource;
import com.teachquest.model.TutoringSession;
import com.teachquest.model.User;
import com.teachquest.model.ChatGroup;
import com.teachquest.repository.ChatGroupRepository;
import com.teachquest.repository.RatingRepository;
import com.teachquest.repository.StudyResourceRepository;
import com.teachquest.repository.TutoringSessionRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final TutoringSessionRepository sessionRepository;
    private final StudyResourceRepository resourceRepository;
    private final RatingRepository ratingRepository;
    private final ChatGroupRepository chatGroupRepository;

    public AdminUserService(UserRepository userRepository, TutoringSessionRepository sessionRepository,
                            StudyResourceRepository resourceRepository, RatingRepository ratingRepository,
                            ChatGroupRepository chatGroupRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.resourceRepository = resourceRepository;
        this.ratingRepository = ratingRepository;
        this.chatGroupRepository = chatGroupRepository;
    }

    public Page<Map<String, Object>> list(String role, String status, String search, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedRole = role == null || role.isBlank() ? null : role.trim();
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();
        return userRepository.searchAdminUsers(normalizedRole, normalizedStatus, normalizedSearch, pageable).map(this::summary);
    }

    public Map<String, Object> detail(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User was not found."));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", summary(user));
        result.put("sessionsTaught", sessionRepository.findByTutorIdOrderByRequestedAtDesc(id).stream().map(this::sessionView).collect(Collectors.toList()));
        result.put("sessionsTaken", sessionRepository.findByStudentIdOrderByRequestedAtDesc(id).stream().map(this::sessionView).collect(Collectors.toList()));
        result.put("resourcesUploaded", resourceRepository.findByUploaderIdOrderByCreatedAtDesc(id).stream().map(this::resourceView).collect(Collectors.toList()));
        result.put("averageRating", "teacher".equalsIgnoreCase(user.getUserType()) || "tutor".equalsIgnoreCase(user.getUserType())
                ? Optional.ofNullable(ratingRepository.averageScoreForTutor(id)).orElse(0D) : null);
        result.put("totalPoints", user.getTotalPoints());
        return result;
    }

    @Transactional
    public User updateStatus(Long id, String status) {
        if (!Set.of("ACTIVE", "SUSPENDED").contains(status)) throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED.");
        User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User was not found."));
        user.setStatus(status);
        if ("SUSPENDED".equals(status)) {
            sessionRepository.cancelPendingForUser(id, java.time.LocalDateTime.now());
            List<ChatGroup> ownedGroups = chatGroupRepository.findByOwnerId(id);
            ownedGroups.forEach(group -> group.setStatus("ARCHIVED"));
            chatGroupRepository.saveAll(ownedGroups);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long id, String role) {
        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        if (!Set.of("student", "teacher", "tutor", "admin").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role must be student, teacher, tutor, or admin.");
        }
        if ("tutor".equals(normalizedRole)) normalizedRole = "teacher";
        User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User was not found."));
        user.setUserType(normalizedRole);
        return userRepository.save(user);
    }

    private Map<String, Object> summary(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("name", user.getUsername());
        view.put("username", user.getUsername());
        view.put("email", user.getEmail());
        view.put("universityId", user.getUniversityId());
        view.put("contactNo", user.getContactNo());
        view.put("address", user.getAddress());
        view.put("role", user.getUserType());
        view.put("registrationDate", user.getCreatedAt());
        view.put("status", user.getStatus());
        view.put("totalPoints", user.getTotalPoints());
        return view;
    }

    private Map<String, Object> sessionView(TutoringSession session) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", session.getId());
        view.put("studentId", session.getStudentId());
        view.put("tutorId", session.getTutorId());
        view.put("courseCode", session.getCourseCode());
        view.put("status", session.getStatus());
        view.put("requestedAt", session.getRequestedAt());
        view.put("completedAt", session.getCompletedAt());
        return view;
    }

    private Map<String, Object> resourceView(StudyResource resource) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", resource.getId());
        view.put("courseCode", resource.getCourseCode());
        view.put("category", resource.getCategory());
        view.put("moderationStatus", resource.getModerationStatus());
        view.put("createdAt", resource.getCreatedAt());
        return view;
    }
}
