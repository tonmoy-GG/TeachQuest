package com.teachquest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachquest.repository.PracticeCourseRepository;
import com.teachquest.repository.PracticeQuizAttemptRepository;
import com.teachquest.repository.PracticeQuizQuestionRepository;
import com.teachquest.repository.QuizLevelProgressRepository;
import com.teachquest.repository.QuizSeriesRepository;
import com.teachquest.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PracticeQuizLevelConfigurationTest {
    @Test
    void exposesTheServerControlledSettingsForAllEnabledLevels() {
        PracticeQuizService service = service(true);

        List<Map<String, Object>> levels = (List<Map<String, Object>>) service.config().get("levels");

        assertEquals(List.of(8, 10, 12, 20, 15),
                levels.stream().map(level -> level.get("questionCount")).toList());
        assertEquals(List.of(600, 900, 1200, 1800, 1500),
                levels.stream().map(level -> level.get("durationSeconds")).toList());
        assertEquals(List.of(70.0, 70.0, 75.0, 80.0, 85.0),
                levels.stream().map(level -> level.get("passThresholdPercent")).toList());
    }

    @Test
    void omitsExpertWhenTheServerFlagIsDisabled() {
        PracticeQuizService service = service(false);

        List<Map<String, Object>> levels = (List<Map<String, Object>>) service.config().get("levels");

        assertEquals(4, levels.size());
        assertEquals(false, service.config().get("expertEnabled"));
    }

    private PracticeQuizService service(boolean expertEnabled) {
        return new PracticeQuizService(
                mock(PracticeCourseRepository.class),
                mock(QuizSeriesRepository.class),
                mock(QuizLevelProgressRepository.class),
                mock(PracticeQuizAttemptRepository.class),
                mock(PracticeQuizQuestionRepository.class),
                mock(UserRepository.class),
                mock(PracticeQuizGenerationService.class),
                mock(ShortAnswerGradingService.class),
                mock(CourseCertificateService.class),
                expertEnabled,
                new ObjectMapper());
    }
}