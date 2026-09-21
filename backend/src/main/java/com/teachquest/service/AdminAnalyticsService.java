package com.teachquest.service;

import com.teachquest.repository.JobApplicationRepository;
import com.teachquest.repository.RatingRepository;
import com.teachquest.repository.ResourceFlagRepository;
import com.teachquest.repository.StudyResourceRepository;
import com.teachquest.repository.TutoringSessionRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AdminAnalyticsService {
    private final UserRepository userRepository;
    private final TutoringSessionRepository sessionRepository;
    private final StudyResourceRepository resourceRepository;
    private final ResourceFlagRepository flagRepository;
    private final JobApplicationRepository applicationRepository;

    public AdminAnalyticsService(UserRepository userRepository, TutoringSessionRepository sessionRepository,
                                 StudyResourceRepository resourceRepository, ResourceFlagRepository flagRepository,
                                 JobApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.resourceRepository = resourceRepository;
        this.flagRepository = flagRepository;
        this.applicationRepository = applicationRepository;
    }

    public Map<String, Object> overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> roleCounts = new HashMap<>();
        userRepository.countByUserType().forEach(row -> roleCounts.put(String.valueOf(row[0]).toLowerCase(), ((Number) row[1]).longValue()));
        result.put("totalUsers", userRepository.count());
        result.put("studentCount", roleCounts.getOrDefault("student", 0L));
        result.put("tutorCount", roleCounts.getOrDefault("teacher", roleCounts.getOrDefault("tutor", 0L)));
        result.put("adminCount", roleCounts.getOrDefault("admin", 0L));
        result.put("activeTutorCount", sessionRepository.countActiveTutorsSince(now.minusDays(30)));
        result.put("completedSessionsThisMonth", sessionRepository.countByStatusAndCompletedAtBetween("COMPLETED", monthStart, now));
        result.put("resourcesUploadedThisMonth", resourceRepository.countByCreatedAtBetween(monthStart, now));
        result.put("unresolvedFlaggedContent", flagRepository.countByStatus("OPEN"));
        result.put("pendingTutorApplications", applicationRepository.countByStatus("PENDING"));
        return result;
    }

    public List<Map<String, Object>> registrations() {
        LocalDate currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstMonday = currentMonday.minusWeeks(7);
        LocalDateTime since = firstMonday.atStartOfDay();
        Map<String, Long> counts = new HashMap<>();
        userRepository.registrationCountsByWeek(since).forEach(row -> counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            LocalDate week = firstMonday.plusWeeks(index);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("weekStart", week.toString());
            point.put("registrations", counts.getOrDefault(week.toString(), 0L));
            result.add(point);
        }
        return result;
    }

    public List<Map<String, Object>> tutoringDemand(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Map<String, Object>> result = new ArrayList<>();
        sessionRepository.topDemandByCourse(safeLimit).forEach(row -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("courseCode", row[0]);
            point.put("requests", ((Number) row[1]).longValue());
            result.add(point);
        });
        return result;
    }
}
