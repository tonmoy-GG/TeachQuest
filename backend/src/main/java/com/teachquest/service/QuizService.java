package com.teachquest.service;

import com.teachquest.model.Quiz;
import com.teachquest.model.QuizOption;
import com.teachquest.model.QuizQuestion;
import com.teachquest.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private com.teachquest.repository.QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private com.teachquest.repository.QuizResultRepository quizResultRepository;

    @Autowired
    private AIService aiService;

    public List<QuizQuestion> getRandomQuestions(int count) {
        return quizQuestionRepository.findRandomQuestions(count);
    }

    public List<QuizQuestion> getQuestionsByType(String type) {
        return quizQuestionRepository.findByQCategory(type);
    }

    public Quiz createQuiz(Quiz quiz) {
        // Ensure relationships are set for cascading
        if (quiz.getQuestions() != null) {
            for (QuizQuestion question : quiz.getQuestions()) {
                question.setQuiz(quiz);
                if (question.getOptions() != null) {
                    for (QuizOption option : question.getOptions()) {
                        option.setQuizQuestion(question);
                    }
                }
            }
        }
        return quizRepository.save(quiz);
    }

    public QuizQuestion saveQuestion(QuizQuestion question) {
        if (question.getQuiz() == null) {
            // Assign to default "Teacher Question Bank" quiz container
            Quiz bank = quizRepository.findByTitle("Teacher Question Bank");
            if (bank == null) {
                bank = new Quiz();
                bank.setTitle("Teacher Question Bank");
                bank.setMarks("0"); // Changed from setTotalMarks(int) to setMarks(String)
                // Set default values for mandatory fields to avoid
                // DataIntegrityViolationException
                bank.setStudentName("SYSTEM_BANK");
                bank.setStudentId("TEACHER_BANK");
                bank = quizRepository.save(bank);
            }
            question.setQuiz(bank);
        }

        if (question.getOptions() != null) {
            for (QuizOption option : question.getOptions()) {
                option.setQuizQuestion(question);
            }
        }
        return quizQuestionRepository.save(question);
    }

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    public Optional<Quiz> getQuizById(Long id) {
        return quizRepository.findById(id);
    }

    public Quiz getQuizByTitle(String title) {
        return quizRepository.findByTitle(title);
    }

    public int calculateScore(Long quizId, java.util.Map<Long, String> submissions) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));
        int score = 0;

        for (com.teachquest.model.QuizQuestion q : quiz.getQuestions()) {
            String submittedOption = submissions.get(q.getId());
            if (submittedOption != null && submittedOption.equalsIgnoreCase(q.getQCorrectAnswer())) {
                score++;
            }
        }
        return score;
    }

    public com.teachquest.model.QuizResult saveResult(String studentId, String studentName, String quizTitle, int score,
            int totalQuestions, int timeTakenSeconds) {
        com.teachquest.model.QuizResult result = new com.teachquest.model.QuizResult();
        result.setStudentId(studentId);
        result.setStudentName(studentName);
        result.setQuizTitle(quizTitle);
        result.setScore(score);
        result.setTotalQuestions(totalQuestions);
        result.setTimeTakenSeconds(timeTakenSeconds);

        // Generate dynamic feedback using AI if possible
        try {
            String feedback = aiService.generatePerformanceFeedback(score, totalQuestions, quizTitle);
            result.setAiFeedback(feedback);
        } catch (Exception e) {
            result.setAiFeedback("Great effort! Keep practicing to improve your score.");
        }

        return quizResultRepository.save(result);
    }

    public List<com.teachquest.model.QuizResult> getStudentHistory(String studentId) {
        return quizResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
    }

    public QuizQuestion updateQuestion(Long questionId, QuizQuestion updatedQuestion) {
        Optional<QuizQuestion> existing = quizQuestionRepository.findById(questionId);
        if (existing.isPresent()) {
            QuizQuestion question = existing.get();
            question.setQuestion(updatedQuestion.getQuestion());
            question.setQCategory(updatedQuestion.getQCategory());
            question.setQCorrectAnswer(updatedQuestion.getQCorrectAnswer());

            // Update options if provided
            if (updatedQuestion.getOptions() != null) {
                question.getOptions().clear();
                for (QuizOption option : updatedQuestion.getOptions()) {
                    option.setQuizQuestion(question);
                    question.addOption(option);
                }
            }
            return quizQuestionRepository.save(question);
        }
        throw new RuntimeException("Question not found with ID: " + questionId);
    }

    public void deleteQuestion(Long questionId) {
        Optional<QuizQuestion> question = quizQuestionRepository.findById(questionId);
        if (question.isPresent()) {
            quizQuestionRepository.deleteById(questionId);
        } else {
            throw new RuntimeException("Question not found with ID: " + questionId);
        }
    }

    public Optional<QuizQuestion> getQuestionById(Long questionId) {
        return quizQuestionRepository.findById(questionId);
    }
}
