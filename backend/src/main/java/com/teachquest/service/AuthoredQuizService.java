package com.teachquest.service;

import com.teachquest.model.*;
import com.teachquest.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthoredQuizService {
    private final QuizRepository quizRepository;
    private final QuizAssignmentRepository assignmentRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final JobPostRepository jobPostRepository;
    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final GroupChatService groupChatService;

    public AuthoredQuizService(QuizRepository quizRepository, QuizAssignmentRepository assignmentRepository,
                               QuizAttemptRepository attemptRepository, QuizAttemptAnswerRepository answerRepository,
                               UserRepository userRepository, JobPostRepository jobPostRepository,
                               ChatGroupRepository groupRepository, ChatGroupMemberRepository groupMemberRepository,
                               GroupChatService groupChatService) {
        this.quizRepository = quizRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.jobPostRepository = jobPostRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupChatService = groupChatService;
    }

    @Transactional
    public Quiz create(QuizCreateCommand command) {
        User tutor = requireTutor(command.authorId());
        String title = clean(command.title());
        if (title.isBlank() || command.questions() == null || command.questions().isEmpty()) {
            throw new IllegalArgumentException("A title and at least one question are required.");
        }
        if (command.attemptsAllowed() < 1) throw new IllegalArgumentException("Attempts allowed must be at least 1.");

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setCourse(clean(command.course()));
        quiz.setTopic(clean(command.topic()));
        quiz.setAuthorId(tutor.getId());
        quiz.setSource("MANUAL");
        quiz.setMarks(String.valueOf(command.questions().size()));
        quiz.setStudentName("ASSIGNED");
        quiz.setStudentId("0");
        quiz.setTimeLimitMinutes(command.timeLimitMinutes() == null || command.timeLimitMinutes() < 1 ? null : command.timeLimitMinutes());
        quiz.setAttemptsAllowed(command.attemptsAllowed());
        quiz.setDueDate(command.dueDate());

        for (QuestionCommand commandQuestion : command.questions()) {
            String type = normalizeType(commandQuestion.type());
            if (clean(commandQuestion.text()).isBlank()) throw new IllegalArgumentException("Every question needs text.");
            if ("MULTIPLE_CHOICE".equals(type) && (commandQuestion.options() == null || commandQuestion.options().size() < 2)) {
                throw new IllegalArgumentException("Multiple-choice questions need at least two options.");
            }
            QuizQuestion question = new QuizQuestion();
            question.setQuestion(commandQuestion.text().trim());
            question.setQuestionType(type);
            question.setQCorrectAnswer(clean(commandQuestion.correctAnswer()));
            question.setQCategory(quiz.getTopic());
            question.setExplanation(clean(commandQuestion.explanation()));
            if (commandQuestion.options() != null) {
                for (int index = 0; index < commandQuestion.options().size(); index++) {
                    QuizOption option = new QuizOption();
                    option.setOptionLabel(String.valueOf((char) ('A' + index)));
                    option.setOptionText(commandQuestion.options().get(index));
                    question.addOption(option);
                }
            }
            quiz.addQuestion(question);
        }
        Quiz saved = quizRepository.save(quiz);
        assign(saved.getId(), new AssignmentCommand(command.assignmentType(), command.studentIds(), command.groupId()), tutor.getId());
        return saved;
    }

    @Transactional
    public void assign(Long quizId, AssignmentCommand command, Long tutorId) {
        Quiz quiz = requireQuiz(quizId);
        requireTutor(tutorId);
        if (!Objects.equals(quiz.getAuthorId(), tutorId)) throw new IllegalArgumentException("Only the quiz author can assign it.");
        String type = clean(command.assignmentType()).toUpperCase(Locale.ROOT);
        if (type.isBlank()) type = "OPEN";
        if ("OPEN".equals(type)) return;
        if ("GROUP".equals(type)) {
            ChatGroup group = groupRepository.findById(command.groupId()).orElseThrow(() -> new IllegalArgumentException("Group was not found."));
            groupChatService.requireOwner(group, tutorId);
            groupMemberRepository.findByGroupId(group.getId()).stream()
                    .filter(member -> !Objects.equals(member.getUserId(), tutorId))
                    .forEach(member -> saveAssignment(quizId, member.getUserId(), group.getId(), "GROUP"));
            return;
        }
        if (!"STUDENTS".equals(type) || command.studentIds() == null || command.studentIds().isEmpty()) {
            throw new IllegalArgumentException("Choose students, a group, or open-course access.");
        }
        for (Long studentId : command.studentIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())) {
            User student = userRepository.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Student account was not found."));
            boolean hired = jobPostRepository.existsByUserIdAndHiredTutorId(studentId, tutorId)
                    || jobPostRepository.existsByUserIdAndHiredTutorIdViaApplication(studentId, tutorId);
            if (!"student".equalsIgnoreCase(student.getUserType()) || !hired) {
                throw new IllegalArgumentException("Every selected student must have hired this tutor.");
            }
            saveAssignment(quizId, studentId, null, "STUDENT");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tutorQuizzes(Long tutorId) {
        requireTutor(tutorId);
        return quizRepository.findByAuthorId(tutorId).stream().map(this::quizSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> studentQuizzes(Long studentId) {
        requireStudent(studentId);
        Set<Long> assigned = assignmentRepository.findByStudentId(studentId).stream().map(QuizAssignment::getQuizId).collect(Collectors.toSet());
        List<Quiz> quizzes = new ArrayList<>(quizRepository.findBySource("MANUAL").stream()
            .filter(quiz -> assigned.contains(quiz.getId()) || assignmentRepository.findByQuizId(quiz.getId()).isEmpty())
                .collect(Collectors.toMap(Quiz::getId, quiz -> quiz, (left, right) -> left)).values());
        return quizzes.stream().map(quiz -> studentSummary(quiz, studentId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForStudent(Long quizId, Long studentId) {
        Quiz quiz = requireQuiz(quizId);
        requireStudentAccess(quiz, studentId);
        Map<String, Object> view = quizSummary(quiz);
        view.put("questions", quiz.getQuestions().stream().map(this::studentQuestion).collect(Collectors.toList()));
        return view;
    }

    @Transactional
    public Map<String, Object> start(Long quizId, Long studentId) {
        Quiz quiz = requireQuiz(quizId);
        requireStudentAccess(quiz, studentId);
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getDueDate() != null && now.isAfter(quiz.getDueDate())) throw new IllegalArgumentException("This quiz is overdue.");
        long count = attemptRepository.countByQuizIdAndStudentId(quizId, studentId);
        if (count >= quiz.getAttemptsAllowed()) throw new IllegalArgumentException("No attempts remaining.");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setStudentId(studentId);
        attempt.setAttemptNumber((int) count + 1);
        attempt.setStartedAt(now);
        if (quiz.getTimeLimitMinutes() != null) attempt.setExpiresAt(now.plusMinutes(quiz.getTimeLimitMinutes()));
        return attemptView(attemptRepository.save(attempt), quiz, false);
    }

    @Transactional
    public Map<String, Object> submit(Long quizId, Long attemptId, Long studentId, Map<String, String> submissions) {
        Quiz quiz = requireQuiz(quizId);
        QuizAttempt attempt = attemptRepository.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt was not found."));
        if (!Objects.equals(attempt.getQuizId(), quizId) || !Objects.equals(attempt.getStudentId(), studentId)) throw new IllegalArgumentException("This attempt does not belong to you.");
        if (!"IN_PROGRESS".equals(attempt.getStatus())) return attemptView(attempt, quiz, true);
        LocalDateTime now = LocalDateTime.now();
        boolean expired = attempt.getExpiresAt() != null && now.isAfter(attempt.getExpiresAt());
        int autoScore = 0;
        boolean needsReview = false;
        Map<String, String> answers = submissions == null ? Collections.emptyMap() : submissions;
        for (QuizQuestion question : quiz.getQuestions()) {
            String answer = answers.get(String.valueOf(question.getId()));
            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttemptId(attemptId);
            attemptAnswer.setQuestionId(question.getId());
            attemptAnswer.setAnswer(answer == null ? "" : answer);
            boolean auto = "MULTIPLE_CHOICE".equals(question.getQuestionType()) || "TRUE_FALSE".equals(question.getQuestionType());
            boolean correct = auto && normalize(answer).equals(normalize(question.getQCorrectAnswer()));
            attemptAnswer.setAwardedScore(correct && !expired ? 1 : 0);
            attemptAnswer.setReviewed(auto);
            if (correct && !expired) autoScore++;
            if (!auto) needsReview = true;
            answerRepository.save(attemptAnswer);
        }
        attempt.setAutoScore(autoScore);
        attempt.setTotalScore(autoScore);
        attempt.setStatus(expired ? "EXPIRED" : needsReview ? "NEEDS_REVIEW" : "COMPLETED");
        attempt.setSubmittedAt(now);
        return attemptView(attemptRepository.save(attempt), quiz, true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> results(Long quizId, Long tutorId) {
        Quiz quiz = requireQuiz(quizId);
        requireTutor(tutorId);
        if (!Objects.equals(quiz.getAuthorId(), tutorId)) throw new IllegalArgumentException("Only the quiz author can view results.");
        return attemptRepository.findByQuizId(quizId).stream().map(attempt -> attemptView(attempt, quiz, true)).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> review(Long attemptId, Long questionId, Long tutorId, int score, String feedback) {
        QuizAttempt attempt = attemptRepository.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt was not found."));
        Quiz quiz = requireQuiz(attempt.getQuizId());
        requireTutor(tutorId);
        if (!Objects.equals(quiz.getAuthorId(), tutorId)) throw new IllegalArgumentException("Only the quiz author can review answers.");
        QuizAttemptAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId).orElseThrow(() -> new IllegalArgumentException("Answer was not found."));
        answer.setAwardedScore(Math.max(0, Math.min(1, score)));
        answer.setFeedback(feedback);
        answer.setReviewed(true);
        answerRepository.save(answer);
        List<QuizAttemptAnswer> answers = answerRepository.findByAttemptId(attemptId);
        attempt.setManualScore(answers.stream().filter(item -> item.getQuestionId().equals(questionId) || item.isReviewed()).mapToInt(QuizAttemptAnswer::getAwardedScore).sum() - attempt.getAutoScore());
        boolean allReviewed = answers.stream().allMatch(QuizAttemptAnswer::isReviewed);
        if (allReviewed) { attempt.setStatus("COMPLETED"); attempt.setTotalScore(answers.stream().mapToInt(QuizAttemptAnswer::getAwardedScore).sum()); }
        attemptRepository.save(attempt);
        return attemptView(attempt, quiz, false);
    }

    private void saveAssignment(Long quizId, Long studentId, Long groupId, String type) {
        if (!assignmentRepository.existsByQuizIdAndStudentId(quizId, studentId)) {
            QuizAssignment assignment = new QuizAssignment(); assignment.setQuizId(quizId); assignment.setStudentId(studentId); assignment.setGroupId(groupId); assignment.setAssignmentType(type); assignmentRepository.save(assignment);
        }
    }

    private void requireStudentAccess(Quiz quiz, Long studentId) {
        requireStudent(studentId);
        boolean assigned = assignmentRepository.existsByQuizIdAndStudentId(quiz.getId(), studentId);
        boolean open = "MANUAL".equalsIgnoreCase(quiz.getSource()) && assignmentRepository.findByQuizId(quiz.getId()).isEmpty();
        if (!assigned && !open) throw new IllegalArgumentException("This quiz is not assigned to you.");
    }

    private User requireTutor(Long id) { User user = requireUser(id); if (!"teacher".equalsIgnoreCase(user.getUserType())) throw new IllegalArgumentException("Only tutors can manage authored quizzes."); return user; }
    private User requireStudent(Long id) { User user = requireUser(id); if (!"student".equalsIgnoreCase(user.getUserType())) throw new IllegalArgumentException("Only students can take quizzes."); return user; }
    private User requireUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User account was not found.")); }
    private Quiz requireQuiz(Long id) { return quizRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Quiz was not found.")); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String normalizeType(String value) { String type = clean(value).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); if (type.equals("MCQ")) type = "MULTIPLE_CHOICE"; if (!Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER").contains(type)) throw new IllegalArgumentException("Unsupported question type."); return type; }
    private String normalize(String value) { return clean(value).toLowerCase(Locale.ROOT); }

    private Map<String, Object> quizSummary(Quiz quiz) { Map<String, Object> view = new LinkedHashMap<>(); view.put("id", quiz.getId()); view.put("title", quiz.getTitle()); view.put("course", quiz.getCourse()); view.put("topic", quiz.getTopic()); view.put("source", quiz.getSource()); view.put("timeLimitMinutes", quiz.getTimeLimitMinutes()); view.put("attemptsAllowed", quiz.getAttemptsAllowed()); view.put("dueDate", quiz.getDueDate()); view.put("questionCount", quiz.getQuestions().size()); return view; }
    private Map<String, Object> studentSummary(Quiz quiz, Long studentId) { Map<String, Object> view = quizSummary(quiz); List<QuizAttempt> attempts = attemptRepository.findByQuizId(quiz.getId()).stream().filter(item -> Objects.equals(item.getStudentId(), studentId)).collect(Collectors.toList()); QuizAttempt latest = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1); String status = latest == null ? (quiz.getDueDate() != null && LocalDateTime.now().isAfter(quiz.getDueDate()) ? "OVERDUE" : "NOT_STARTED") : latest.getStatus(); view.put("status", status); view.put("score", latest == null ? null : latest.getTotalScore()); return view; }
    private Map<String, Object> studentQuestion(QuizQuestion question) { Map<String, Object> view = new LinkedHashMap<>(); view.put("id", question.getId()); view.put("question", question.getQuestion()); view.put("questionType", question.getQuestionType()); view.put("options", question.getOptions()); view.put("explanation", question.getExplanation()); return view; }
    private Map<String, Object> attemptView(QuizAttempt attempt, Quiz quiz, boolean includeAnswers) { Map<String, Object> view = new LinkedHashMap<>(); view.put("id", attempt.getId()); view.put("quizId", attempt.getQuizId()); view.put("studentId", attempt.getStudentId()); view.put("status", attempt.getStatus()); view.put("attemptNumber", attempt.getAttemptNumber()); view.put("startedAt", attempt.getStartedAt()); view.put("expiresAt", attempt.getExpiresAt()); view.put("submittedAt", attempt.getSubmittedAt()); view.put("autoScore", attempt.getAutoScore()); view.put("manualScore", attempt.getManualScore()); view.put("totalScore", attempt.getTotalScore()); if (includeAnswers) { view.put("questions", quiz.getQuestions().stream().map(question -> { Map<String, Object> item = studentQuestion(question); item.put("correctAnswer", question.getQCorrectAnswer()); return item; }).collect(Collectors.toList())); view.put("answers", answerRepository.findByAttemptId(attempt.getId())); } return view; }

    public record QuizCreateCommand(Long authorId, String title, String course, String topic, Integer timeLimitMinutes, int attemptsAllowed, java.time.LocalDateTime dueDate, String assignmentType, List<Long> studentIds, Long groupId, List<QuestionCommand> questions) {}
    public record QuestionCommand(String text, String type, List<String> options, String correctAnswer, String explanation) {}
    public record AssignmentCommand(String assignmentType, List<Long> studentIds, Long groupId) {}
}
