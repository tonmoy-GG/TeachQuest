package com.teachquest.controller;

import com.teachquest.service.PracticeQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice-quizzes")
@CrossOrigin(origins = "*")
public class PracticeQuizController {
    private final PracticeQuizService practiceQuizService;

    public PracticeQuizController(PracticeQuizService practiceQuizService) {
        this.practiceQuizService = practiceQuizService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> config() {
        return execute(practiceQuizService::config);
    }

    @GetMapping("/courses")
    public ResponseEntity<?> courses(@RequestParam Long userId) {
        return execute(() -> practiceQuizService.courses(userId));
    }

    @GetMapping("/progress")
    public ResponseEntity<?> progress(@RequestParam Long userId) {
        return execute(() -> practiceQuizService.progress(userId));
    }

    @GetMapping("/courses/{courseId}/history")
    public ResponseEntity<?> history(@PathVariable Long courseId, @RequestParam Long userId,
                                    @RequestParam(required = false) Integer level) {
        return execute(() -> practiceQuizService.history(userId, courseId, level));
    }

    @PostMapping("/courses/{courseId}/attempts")
    public ResponseEntity<?> start(@PathVariable Long courseId, @RequestParam Long userId,
                                   @RequestBody PracticeQuizService.StartRequest request) {
        return execute(() -> practiceQuizService.start(userId, courseId, request));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<?> attempt(@PathVariable Long attemptId, @RequestParam Long userId) {
        return execute(() -> practiceQuizService.getAttempt(userId, attemptId));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<?> submit(@PathVariable Long attemptId, @RequestParam Long userId,
                                    @RequestBody PracticeQuizService.SubmitRequest request) {
        return execute(() -> practiceQuizService.submit(userId, attemptId, request));
    }

    private ResponseEntity<?> execute(CheckedSupplier supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (Exception exception) {
            return ResponseEntity.status(503).body("Practice quiz request failed: " + exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier {
        Object get() throws Exception;
    }
}