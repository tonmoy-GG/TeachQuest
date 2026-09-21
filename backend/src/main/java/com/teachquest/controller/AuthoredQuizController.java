package com.teachquest.controller;

import com.teachquest.service.AuthoredQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/authored-quizzes")
@CrossOrigin(origins = "*")
public class AuthoredQuizController {
    private final AuthoredQuizService service;

    public AuthoredQuizController(AuthoredQuizService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AuthoredQuizService.QuizCreateCommand command) { return execute(() -> service.create(command)); }

    @PostMapping("/{quizId}/assign")
    public ResponseEntity<?> assign(@PathVariable Long quizId, @RequestParam Long tutorId, @RequestBody AuthoredQuizService.AssignmentCommand command) { return execute(() -> { service.assign(quizId, command, tutorId); return Collections.singletonMap("ok", true); }); }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> tutorQuizzes(@PathVariable Long tutorId) { return execute(() -> service.tutorQuizzes(tutorId)); }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> studentQuizzes(@PathVariable Long studentId) { return execute(() -> service.studentQuizzes(studentId)); }

    @GetMapping("/{quizId}")
    public ResponseEntity<?> getForStudent(@PathVariable Long quizId, @RequestParam Long studentId) { return execute(() -> service.getForStudent(quizId, studentId)); }

    @PostMapping("/{quizId}/attempts")
    public ResponseEntity<?> start(@PathVariable Long quizId, @RequestParam Long studentId) { return execute(() -> service.start(quizId, studentId)); }

    @PostMapping("/{quizId}/attempts/{attemptId}/submit")
    public ResponseEntity<?> submit(@PathVariable Long quizId, @PathVariable Long attemptId, @RequestParam Long studentId, @RequestBody Map<String, String> submissions) { return execute(() -> service.submit(quizId, attemptId, studentId, submissions)); }

    @GetMapping("/{quizId}/results")
    public ResponseEntity<?> results(@PathVariable Long quizId, @RequestParam Long tutorId) { return execute(() -> service.results(quizId, tutorId)); }

    @PostMapping("/attempts/{attemptId}/review")
    public ResponseEntity<?> review(@PathVariable Long attemptId, @RequestParam Long questionId, @RequestParam Long tutorId, @RequestParam int score, @RequestParam(required = false) String feedback) { return execute(() -> service.review(attemptId, questionId, tutorId, score, feedback)); }

    private ResponseEntity<?> execute(CheckedSupplier supplier) {
        try { return ResponseEntity.ok(supplier.get()); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(403).body(e.getMessage()); }
        catch (Exception e) { return ResponseEntity.badRequest().body("Authored quiz request failed: " + e.getMessage()); }
    }

    @FunctionalInterface private interface CheckedSupplier { Object get() throws Exception; }
}
