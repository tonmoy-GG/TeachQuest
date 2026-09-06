package com.teachquest.controller;

import com.teachquest.model.Quiz;
import com.teachquest.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @PostMapping("/create")
    public ResponseEntity<Quiz> createQuiz(@RequestBody Quiz quiz) {
        Quiz createdQuiz = quizService.createQuiz(quiz);
        return ResponseEntity.ok(createdQuiz);
    }

    @PostMapping("/question")
    public ResponseEntity<com.teachquest.model.QuizQuestion> createQuestion(
            @RequestBody com.teachquest.model.QuizQuestion question) {
        return ResponseEntity.ok(quizService.saveQuestion(question));
    }

    @GetMapping("/all")
    public List<Quiz> getAllQuizzes() {
        return quizService.getAllQuizzes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return quizService.getQuizById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Integer> submitQuiz(@PathVariable Long id,
            @RequestBody java.util.Map<Long, String> submissions) {
        int score = quizService.calculateScore(id, submissions);
        return ResponseEntity.ok(score);
    }

    @GetMapping("/random")
    public List<com.teachquest.model.QuizQuestion> getRandomQuestions(@RequestParam(defaultValue = "10") int count) {
        return quizService.getRandomQuestions(count);
    }

    @Autowired
    private com.teachquest.service.AIService aiService;

    @GetMapping("/by-type")
    public List<com.teachquest.model.QuizQuestion> getQuestionsByType(@RequestParam String type) {
        return quizService.getQuestionsByType(type);
    }

    @PostMapping("/evaluate-answer")
    public ResponseEntity<java.util.Map<String, Object>> evaluateAnswer(
            @RequestBody java.util.Map<String, String> payload) {
        String question = payload.get("question");
        String userAnswer = payload.get("userAnswer");
        String correctAnswer = payload.get("correctAnswer");

        java.util.Map<String, Object> result = aiService.evaluateShortAnswer(question, userAnswer, correctAnswer);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/evaluate-bulk")
    public ResponseEntity<List<java.util.Map<String, Object>>> evaluateAnswersBulk(
            @RequestBody List<java.util.Map<String, String>> answers) {
        return ResponseEntity.ok(aiService.evaluateMultipleAnswers(answers));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateAIQuiz(@RequestBody java.util.Map<String, Object> payload) {
        try {
            String course = (String) payload.get("course");
            String difficulty = (String) payload.get("difficulty");
            String type = (String) payload.get("type");
            int count = ((Number) payload.get("count")).intValue();

            List<java.util.Map<String, Object>> questions = aiService.generateQuizQuestions(course, difficulty, type,
                    count);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("AI Error: " + e.getMessage());
        }
    }

    @PostMapping("/save-result")
    public ResponseEntity<com.teachquest.model.QuizResult> saveQuizResult(
            @RequestBody java.util.Map<String, Object> payload) {
        String studentId = (String) payload.get("studentId");
        String studentName = (String) payload.get("studentName");
        String quizTitle = (String) payload.get("quizTitle");
        int score = ((Number) payload.get("score")).intValue();
        int totalQuestions = ((Number) payload.get("totalQuestions")).intValue();
        int timeTakenSeconds = ((Number) payload.get("timeTakenSeconds")).intValue();

        com.teachquest.model.QuizResult result = quizService.saveResult(studentId, studentName, quizTitle, score,
                totalQuestions, timeTakenSeconds);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{studentId}")
    public ResponseEntity<List<com.teachquest.model.QuizResult>> getHistory(@PathVariable String studentId) {
        return ResponseEntity.ok(quizService.getStudentHistory(studentId));
    }

    @DeleteMapping("/question/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            quizService.deleteQuestion(questionId);
            return ResponseEntity.ok("Question deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting question: " + e.getMessage());
        }
    }

    @PutMapping("/question/{questionId}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody com.teachquest.model.QuizQuestion updatedQuestion) {
        try {
            com.teachquest.model.QuizQuestion saved = quizService.updateQuestion(questionId, updatedQuestion);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating question: " + e.getMessage());
        }
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<?> getQuestion(@PathVariable Long questionId) {
        try {
            return quizService.getQuestionById(questionId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrieving question: " + e.getMessage());
        }
    }

    @GetMapping("/teacher-bank")
    public ResponseEntity<List<com.teachquest.model.QuizQuestion>> getTeacherQuestionBank() {
        try {
            Quiz bank = quizService.getQuizByTitle("Teacher Question Bank");
            if (bank != null) {
                return ResponseEntity.ok(bank.getQuestions());
            }
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }
}
