package com.teachquest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachquest.model.CourseCertificate;
import com.teachquest.model.PracticeCourse;
import com.teachquest.model.PracticeQuizAttempt;
import com.teachquest.model.QuizLevelProgress;
import com.teachquest.repository.CourseCertificateRepository;
import com.teachquest.repository.PracticeCourseRepository;
import com.teachquest.repository.PracticeQuizAttemptRepository;
import com.teachquest.repository.QuizLevelProgressRepository;
import com.teachquest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCertificateServiceTest {
    @Mock private CourseCertificateRepository certificateRepository;
    @Mock private PracticeQuizAttemptRepository attemptRepository;
    @Mock private QuizLevelProgressRepository progressRepository;
    @Mock private PracticeCourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PracticeCourse course;

    @BeforeEach
    void setUp() {
        course = new PracticeCourse();
        course.setName("Data Structures");
        QuizLevelProgress progress = new QuizLevelProgress();
        progress.setHighestUnlockedLevel(4);
        lenient().when(progressRepository.findByUserIdAndCourseIdForUpdate(1L, 2L))
                .thenReturn(Optional.of(progress));
        lenient().when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        lenient().when(certificateRepository.findFirstByUserIdAndCourseIdAndStatusOrderByIssuedAtDesc(1L, 2L, "CURRENT"))
                .thenReturn(Optional.empty());
        lenient().when(certificateRepository.save(any(CourseCertificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void averagesBestPassingScoresAndIgnoresFailures() {
        when(attemptRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(List.of(
                attempt(1, 99, false), attempt(1, 72, true), attempt(1, 80, true),
                attempt(2, 85, true), attempt(3, 90, true), attempt(4, 87, true)));

        Map<String, Object> issued = service(false).issueIfImproved(1L, 2L).orElseThrow();

        assertEquals("Silver", issued.get("tier"));
        assertEquals(85.5, ((Number) issued.get("averageScore")).doubleValue(), 0.001);
        Map<?, ?> breakdown = (Map<?, ?>) issued.get("scoreBreakdown");
        assertEquals(80.0, ((Number) breakdown.get(1)).doubleValue(), 0.001);
        verify(eventPublisher).publishEvent(any(CertificateIssuedEvent.class));
    }

    @Test
    void passingExpertAwardsGoldEvenWhenAverageIsBelowNinetyFive() {
        when(attemptRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(List.of(
                attempt(1, 60, true), attempt(2, 70, true), attempt(3, 70, true),
                attempt(4, 70, true), attempt(5, 71, true)));

        Map<String, Object> issued = service(true).issueIfImproved(1L, 2L).orElseThrow();

        assertEquals("Gold", issued.get("tier"));
        assertTrue(((Map<?, ?>) issued.get("scoreBreakdown")).containsKey(5));
    }

    @Test
    void doesNotIssueUntilAllRequiredLevelsHavePassingAttempts() {
        when(attemptRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(List.of(
                attempt(1, 90, true), attempt(2, 90, true), attempt(3, 90, true),
                attempt(4, 99, false)));

        assertTrue(service(false).issueIfImproved(1L, 2L).isEmpty());
    }

    @Test
    void improvedAverageArchivesThePreviousCertificate() {
        CourseCertificate previous = new CourseCertificate();
        previous.setCertificateId("old-id");
        previous.setTier("BRONZE");
        previous.setAverageScore(75);
        previous.setStatus("CURRENT");
        when(certificateRepository.findFirstByUserIdAndCourseIdAndStatusOrderByIssuedAtDesc(1L, 2L, "CURRENT"))
                .thenReturn(Optional.of(previous));
        when(attemptRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(List.of(
                attempt(1, 80, true), attempt(2, 80, true), attempt(3, 80, true), attempt(4, 80, true)));

        Map<String, Object> reissued = service(false).issueIfImproved(1L, 2L).orElseThrow();

        assertEquals("SUPERSEDED", previous.getStatus());
        assertNotEquals("old-id", reissued.get("certificateId"));
        assertFalse(reissued.get("certificateId").toString().isBlank());
    }

        @Test
        void firstExpertPassReissuesAnExistingGoldCertificate() {
                CourseCertificate previous = new CourseCertificate();
                previous.setCertificateId("old-gold-id");
                previous.setTier("GOLD");
                previous.setAverageScore(96);
                previous.setScoreBreakdownJson("{\"1\":96,\"2\":96,\"3\":96,\"4\":96}");
                previous.setStatus("CURRENT");
                when(certificateRepository.findFirstByUserIdAndCourseIdAndStatusOrderByIssuedAtDesc(1L, 2L, "CURRENT"))
                                .thenReturn(Optional.of(previous));
                when(attemptRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(List.of(
                                attempt(1, 100, true), attempt(2, 100, true), attempt(3, 100, true),
                                attempt(4, 100, true), attempt(5, 60, true)));

                Map<String, Object> reissued = service(true).issueIfImproved(1L, 2L).orElseThrow();

                assertEquals("Gold", reissued.get("tier"));
                assertEquals(92.0, ((Number) reissued.get("averageScore")).doubleValue(), 0.001);
                assertEquals("SUPERSEDED", previous.getStatus());
        }

    private CourseCertificateService service(boolean expertEnabled) {
        return new CourseCertificateService(certificateRepository, attemptRepository, progressRepository,
                courseRepository, userRepository, new ObjectMapper(), eventPublisher, expertEnabled,
                "http://localhost:5173");
    }

    private PracticeQuizAttempt attempt(int level, double score, boolean passed) {
        PracticeQuizAttempt attempt = new PracticeQuizAttempt();
        attempt.setLevel(level);
        attempt.setScorePercent(score);
        attempt.setPassed(passed);
        return attempt;
    }
}