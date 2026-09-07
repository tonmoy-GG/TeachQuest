package com.teachquest.controller;

import com.teachquest.model.Answer;
import com.teachquest.model.QuestionPost;
import com.teachquest.model.User;
import com.teachquest.repository.AnswerRepository;
import com.teachquest.repository.QuestionPostRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionPostRepository questionPostRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public QuestionController(QuestionPostRepository questionPostRepository,
                               AnswerRepository answerRepository,
                               UserRepository userRepository) {
        this.questionPostRepository = questionPostRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getQuestions() {
        return questionPostRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::questionSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        Optional<QuestionPost> question = questionPostRepository.findById(id);
        if (question.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = questionSummary(question.get());
        result.put("answers", answerRepository.findByQuestionIdOrderByCreatedAtAsc(id).stream()
                .map(this::answerView)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody QuestionRequest request) {
        String title = clean(request.getTitle());
        String body = clean(request.getBody());
        if (title.isEmpty() || body.isEmpty()) {
            return ResponseEntity.badRequest().body("Title and question details are required.");
        }

        QuestionPost question = new QuestionPost();
        question.setTitle(title);
        question.setBody(body);
        question.setCategory(clean(request.getCategory()).isEmpty() ? "General" : clean(request.getCategory()));
        question.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(questionSummary(questionPostRepository.save(question)));
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<?> createAnswer(@PathVariable Long id, @RequestBody AnswerRequest request) {
        if (!questionPostRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        String email = clean(request.getEmail()).toLowerCase();
        String body = clean(request.getBody());
        if (email.isEmpty() || body.isEmpty()) {
            return ResponseEntity.badRequest().body("A registered account and answer are required.");
        }

        Optional<User> registeredUser = userRepository.findByEmailIgnoreCase(email).stream().findFirst();
        if (registeredUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only registered users can answer questions.");
        }

        Answer answer = new Answer();
        answer.setQuestionId(id);
        answer.setUserId(registeredUser.get().getId());
        answer.setBody(body);
        answer.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(answerView(answerRepository.save(answer)));
    }

    private Map<String, Object> questionSummary(QuestionPost question) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", question.getId());
        result.put("title", question.getTitle());
        result.put("body", question.getBody());
        result.put("category", question.getCategory());
        result.put("createdAt", question.getCreatedAt());
        result.put("answerCount", answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId()).size());
        return result;
    }

    private Map<String, Object> answerView(Answer answer) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", answer.getId());
        result.put("body", answer.getBody());
        result.put("createdAt", answer.getCreatedAt());
        userRepository.findById(answer.getUserId()).ifPresent(user -> result.put("author", user.getUsername()));
        return result;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static class QuestionRequest {
        private String title;
        private String body;
        private String category;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class AnswerRequest {
        private String email;
        private String body;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}
