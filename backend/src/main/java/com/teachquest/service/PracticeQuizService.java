package com.teachquest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachquest.model.PracticeCourse;
import com.teachquest.model.PracticeQuizAttempt;
import com.teachquest.model.PracticeQuizQuestion;
import com.teachquest.model.QuizLevelProgress;
import com.teachquest.model.QuizSeries;
import com.teachquest.model.User;
import com.teachquest.repository.PracticeCourseRepository;
import com.teachquest.repository.PracticeQuizAttemptRepository;
import com.teachquest.repository.PracticeQuizQuestionRepository;
import com.teachquest.repository.QuizLevelProgressRepository;
import com.teachquest.repository.QuizSeriesRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PracticeQuizService {
    private static final int MAX_ATTEMPTS_PER_HOUR = 10;
    private static final int COOLDOWN_SECONDS = 30;
    private static final Map<Integer, LevelConfiguration> LEVEL_CONFIGURATIONS = Map.of(
            1, new LevelConfiguration("Easy", "Recall and core concepts", 8, 600, 70),
            2, new LevelConfiguration("Medium", "Apply concepts to problems", 10, 900, 70),
            3, new LevelConfiguration("Hard", "Reason across concepts and edge cases", 12, 1200, 75),
            4, new LevelConfiguration("Mock Test", "Mixed, exam-style challenge", 20, 1800, 80),
            5, new LevelConfiguration("Expert", "Optional bonus challenge", 15, 1500, 85));

    private final PracticeCourseRepository courseRepository;
    private final QuizSeriesRepository seriesRepository;
    private final QuizLevelProgressRepository progressRepository;
    private final PracticeQuizAttemptRepository attemptRepository;
    private final PracticeQuizQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final PracticeQuizGenerationService generationService;
    private final ShortAnswerGradingService gradingService;
    private final CourseCertificateService certificateService;
    private final ObjectMapper objectMapper;
    private final boolean expertEnabled;

    public PracticeQuizService(PracticeCourseRepository courseRepository,
                               QuizSeriesRepository seriesRepository,
                               QuizLevelProgressRepository progressRepository,
                               PracticeQuizAttemptRepository attemptRepository,
                               PracticeQuizQuestionRepository questionRepository,
                               UserRepository userRepository,
                               PracticeQuizGenerationService generationService,
                               ShortAnswerGradingService gradingService,
                               CourseCertificateService certificateService,
                               @Value("${teachquest.practice.expert-enabled:false}") boolean expertEnabled,
                               ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.seriesRepository = seriesRepository;
        this.progressRepository = progressRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.generationService = generationService;
        this.gradingService = gradingService;
        this.certificateService = certificateService;
        this.expertEnabled = expertEnabled;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> config() {
        List<Map<String, Object>> levels = new ArrayList<>();
        for (int level = 1; level <= (expertEnabled ? 5 : 4); level++) {
            LevelConfiguration configuration = LEVEL_CONFIGURATIONS.get(level);
            levels.add(Map.of(
                    "level", level,
                    "name", configuration.name(),
                    "detail", configuration.detail(),
                    "questionCount", configuration.questionCount(),
                    "durationSeconds", configuration.durationSeconds(),
                    "passThresholdPercent", configuration.passThresholdPercent()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("levels", levels);
        result.put("expertEnabled", expertEnabled);
        return result;
    }

    public List<PracticeCourse> courses(Long userId) {
        requireLearner(userId);
        return courseRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Map<String, Object>> progress(Long userId) {
        requireLearner(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeCourse course : courseRepository.findByActiveTrueOrderByNameAsc()) {
            QuizLevelProgress progress = progressRepository.findByUserIdAndCourseId(userId, course.getId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("course", course);
            item.put("highestUnlockedLevel", progress == null ? 1 : effectiveHighestUnlockedLevel(progress));
            item.put("status", progress == null ? "IN_PROGRESS" : progress.getStatus());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> history(Long userId, Long courseId, Integer level) {
        requireLearner(userId);
        requireCourse(courseId);
        List<PracticeQuizAttempt> attempts = level == null
                ? attemptRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(attempt -> attempt.getCourseId().equals(courseId)).toList()
                : attemptRepository.findByUserIdAndCourseIdAndLevelOrderByCreatedAtDesc(userId, courseId, level);
        return attempts.stream().map(this::attemptSummary).toList();
    }

    public Map<String, Object> start(Long userId, Long courseId, StartRequest request) {
        User user = requireLearner(userId);
        PracticeCourse course = requireCourse(courseId);
        validateSettings(request);
        enforceRateLimit(userId);
        closeExpiredAttempt(userId);
        attemptRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "IN_PROGRESS")
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Resume or submit your in-progress quiz before generating another.");
            });

        QuizLevelProgress progress = progressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> {
                    QuizLevelProgress initial = new QuizLevelProgress();
                    initial.setUserId(userId);
                    initial.setCourseId(courseId);
                    return progressRepository.save(initial);
                });
        if (request.level() > effectiveHighestUnlockedLevel(progress)) {
            throw new IllegalArgumentException("Pass the previous level to unlock this quiz.");
        }
        LevelConfiguration configuration = LEVEL_CONFIGURATIONS.get(request.level());

        String topic = request.topic().trim();
        QuizSeries series = seriesRepository.findByUserIdAndCourseIdAndTopicIgnoreCase(userId, courseId, topic)
                .orElseGet(QuizSeries::new);
        series.setUserId(userId);
        series.setCourseId(courseId);
        series.setTopic(topic);
        series.setQuestionCount(configuration.questionCount());
        series.setDurationSeconds(configuration.durationSeconds());
        series.setQuestionMix(request.questionMix());
        series.setPassThresholdPercent(configuration.passThresholdPercent());
        series = seriesRepository.save(series);

        PracticeQuizAttempt attempt = new PracticeQuizAttempt();
        attempt.setSeriesId(series.getId());
        attempt.setUserId(userId);
        attempt.setCourseId(courseId);
        attempt.setLevel(request.level());
        attempt.setQuestionCount(configuration.questionCount());
        attempt.setDurationSeconds(configuration.durationSeconds());
        attempt.setPassThresholdPercent(series.getPassThresholdPercent());
        attempt.setStatus("GENERATING");
        attempt = attemptRepository.save(attempt);

        List<PracticeQuizGenerationService.GeneratedQuestion> generated;
        try {
            generated = generationService.generate(course.getName(), topic, request.level(), configuration.questionCount(), request.questionMix());
        } catch (RuntimeException exception) {
            attempt.setStatus("GENERATION_FAILED");
            attempt.setSubmittedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
            throw exception;
        }

        List<PracticeQuizQuestion> storedQuestions = new ArrayList<>();
        for (int index = 0; index < generated.size(); index++) {
            PracticeQuizGenerationService.GeneratedQuestion source = generated.get(index);
            PracticeQuizQuestion question = new PracticeQuizQuestion();
            question.setAttemptId(attempt.getId());
            question.setQuestionOrder(index + 1);
            question.setQuestionText(source.question());
            question.setQuestionType(source.type());
            question.setOptionsJson(source.type().equals("MCQ") ? writeJson(source.options()) : null);
            question.setCorrectAnswer(source.correctOptionId());
            question.setExpectedAnswer(source.expectedAnswer());
            question.setFeedback(source.explanation());
            storedQuestions.add(question);
        }
        questionRepository.saveAll(storedQuestions);
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now());
        attempt = attemptRepository.save(attempt);
        return attemptView(attempt, false);
    }

    public Map<String, Object> getAttempt(Long userId, Long attemptId) {
        requireLearner(userId);
        PracticeQuizAttempt attempt = ownedAttempt(userId, attemptId);
        return attemptView(attempt, attempt.getStatus().equals("SUBMITTED") || attempt.getStatus().equals("EXPIRED"));
    }

    @Transactional
    public Map<String, Object> submit(Long userId, Long attemptId, SubmitRequest request) {
        requireLearner(userId);
        PracticeQuizAttempt attempt = attemptRepository.findOwnedAttemptForUpdate(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found."));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalArgumentException("This quiz attempt has already been submitted.");
        }
        List<PracticeQuizQuestion> questions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attemptId);
        Map<Long, String> answers = new LinkedHashMap<>();
        if (request != null && request.answers() != null) {
            for (AnswerSubmission answer : request.answers()) {
                if (answer == null || answer.questionId() == null || answers.put(answer.questionId(), cleanAnswer(answer.answer())) != null) {
                    throw new IllegalArgumentException("Each question can only have one answer.");
                }
            }
        }
        Set<Long> allowedIds = new HashSet<>();
        for (PracticeQuizQuestion question : questions) allowedIds.add(question.getId());
        if (!allowedIds.containsAll(answers.keySet())) throw new IllegalArgumentException("An answer refers to another attempt.");

        LocalDateTime now = LocalDateTime.now();
        boolean expired = attempt.getStartedAt().plusSeconds(attempt.getDurationSeconds()).isBefore(now);
        double earned = 0;
        if (!expired) {
            for (PracticeQuizQuestion question : questions) {
                String answer = answers.getOrDefault(question.getId(), "");
                question.setStudentAnswer(answer);
                if ("MCQ".equals(question.getQuestionType())) {
                    boolean correct = question.getCorrectAnswer() != null && question.getCorrectAnswer().equalsIgnoreCase(answer.trim());
                    question.setScorePercent(correct ? 100.0 : 0.0);
                    question.setCorrect(correct);
                    question.setFeedback(correct ? "Correct." : "Incorrect. The correct option is " + question.getCorrectAnswer() + ".");
                } else if (answer.isBlank()) {
                    question.setScorePercent(0.0);
                    question.setCorrect(false);
                    question.setFeedback("No answer was submitted.");
                } else {
                    ShortAnswerGradingService.Grade grade = gradingService.grade(
                            question.getQuestionText(), question.getExpectedAnswer(), answer);
                    question.setScorePercent(grade.score());
                    question.setCorrect(grade.score() >= 70);
                    question.setFeedback(grade.feedback());
                }
                earned += question.getScorePercent();
                questionRepository.save(question);
            }
        }

        double score = expired || questions.isEmpty() ? 0 : earned / questions.size();
        boolean passed = !expired && score >= attempt.getPassThresholdPercent();
        attempt.setScorePercent(score);
        attempt.setPassed(passed);
        attempt.setSubmittedAt(now);
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        attemptRepository.save(attempt);

        if (passed) {
            QuizLevelProgress progress = progressRepository.findByUserIdAndCourseId(userId, attempt.getCourseId())
                    .orElseThrow(() -> new IllegalStateException("Quiz progress was not initialized."));
            if (attempt.getLevel() == 4) {
                progress.setHighestUnlockedLevel(expertEnabled ? 5 : 4);
                progress.setStatus("MASTERED");
            } else if (attempt.getLevel() < 4) {
                progress.setHighestUnlockedLevel(Math.max(progress.getHighestUnlockedLevel(), attempt.getLevel() + 1));
            }
            progressRepository.save(progress);
        }
        Map<String, Object> result = attemptView(attempt, true);
        if (passed) {
            certificateService.issueIfImproved(userId, attempt.getCourseId())
                    .ifPresent(certificate -> result.put("certificate", certificate));
        }
        return result;
    }

    private User requireLearner(Long userId) {
        if (userId == null) throw new IllegalArgumentException("A user ID is required.");
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        if ("admin".equalsIgnoreCase(user.getUserType())) throw new IllegalArgumentException("Practice quizzes are for students and tutors.");
        return user;
    }

    private PracticeCourse requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .filter(PracticeCourse::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
    }

    private int effectiveHighestUnlockedLevel(QuizLevelProgress progress) {
        if (expertEnabled && "MASTERED".equals(progress.getStatus())) return 5;
        return progress.getHighestUnlockedLevel();
    }

    private PracticeQuizAttempt ownedAttempt(Long userId, Long attemptId) {
        return attemptRepository.findById(attemptId)
                .filter(attempt -> attempt.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found."));
    }

    private void validateSettings(StartRequest request) {
        if (request == null || request.topic() == null || request.topic().isBlank() || request.topic().length() > 120) {
            throw new IllegalArgumentException("Enter a topic up to 120 characters.");
        }
        if (request.level() < 1 || request.level() > (expertEnabled ? 5 : 4)) {
            throw new IllegalArgumentException("This quiz level is not enabled.");
        }
        if (!List.of("MCQ_ONLY", "SHORT_ANSWER_ONLY", "BOTH").contains(request.questionMix())) {
            throw new IllegalArgumentException("Choose a supported question mix.");
        }
    }

    private void enforceRateLimit(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (attemptRepository.countByUserIdAndCreatedAtAfter(userId, now.minusHours(1)) >= MAX_ATTEMPTS_PER_HOUR) {
            throw new IllegalArgumentException("You have reached the limit of 10 quiz attempts per hour.");
        }
        attemptRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(last -> {
            if (last.getCreatedAt().plusSeconds(COOLDOWN_SECONDS).isAfter(now)) {
                throw new IllegalArgumentException("Please wait 30 seconds before generating another quiz.");
            }
        });
    }

    private void closeExpiredAttempt(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        attemptRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "IN_PROGRESS")
                .filter(attempt -> attempt.getStartedAt() != null
                        && !attempt.getStartedAt().plusSeconds(attempt.getDurationSeconds()).isAfter(now))
                .ifPresent(attempt -> {
                    attempt.setStatus("EXPIRED");
                    attempt.setScorePercent(0.0);
                    attempt.setPassed(false);
                    attempt.setSubmittedAt(now);
                    attemptRepository.save(attempt);
                });
    }

    private String cleanAnswer(String answer) {
        if (answer == null) return "";
        if (answer.length() > 4000) throw new IllegalArgumentException("Answers must be 4,000 characters or fewer.");
        return answer.trim();
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Unable to store generated quiz options.", exception); }
    }

    private Map<String, Object> attemptView(PracticeQuizAttempt attempt, boolean revealAnswers) {
        Map<String, Object> result = attemptSummary(attempt);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (PracticeQuizQuestion question : questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", question.getId());
            item.put("order", question.getQuestionOrder());
            item.put("question", question.getQuestionText());
            item.put("type", question.getQuestionType());
            item.put("options", readOptions(question.getOptionsJson()));
            if (revealAnswers) {
                item.put("answer", question.getStudentAnswer());
                item.put("scorePercent", question.getScorePercent());
                item.put("correct", question.getCorrect());
                item.put("feedback", question.getFeedback());
                if ("MCQ".equals(question.getQuestionType())) item.put("correctOptionId", question.getCorrectAnswer());
                else item.put("expectedAnswer", question.getExpectedAnswer());
            }
            questions.add(item);
        }
        result.put("questions", questions);
        return result;
    }

    private List<PracticeQuizGenerationService.GeneratedOption> readOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalStateException("Stored quiz options could not be read.", exception); }
    }

    private Map<String, Object> attemptSummary(PracticeQuizAttempt attempt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", attempt.getId());
        item.put("courseId", attempt.getCourseId());
        item.put("level", attempt.getLevel());
        item.put("status", attempt.getStatus());
        item.put("questionCount", attempt.getQuestionCount());
        item.put("durationSeconds", attempt.getDurationSeconds());
        item.put("createdAt", attempt.getCreatedAt());
        item.put("startedAt", attempt.getStartedAt());
        item.put("submittedAt", attempt.getSubmittedAt());
        item.put("scorePercent", attempt.getScorePercent());
        item.put("passed", attempt.getPassed());
        item.put("passThresholdPercent", attempt.getPassThresholdPercent());
        return item;
    }

    public record StartRequest(int level, String topic, String questionMix) { }

    private record LevelConfiguration(String name, String detail, int questionCount,
                                       int durationSeconds, double passThresholdPercent) { }
    public record AnswerSubmission(Long questionId, String answer) { }
    public record SubmitRequest(List<AnswerSubmission> answers) { }
}