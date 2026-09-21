package com.teachquest.controller;

import com.teachquest.service.AdminAccessService;
import com.teachquest.service.AdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AdminAnalyticsController {
    private final AdminAccessService accessService;
    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAccessService accessService, AdminAnalyticsService analyticsService) {
        this.accessService = accessService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            accessService.requireAdmin(userId);
            return ResponseEntity.ok(analyticsService.overview());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        }
    }

    @GetMapping("/registrations")
    public ResponseEntity<?> registrations(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            accessService.requireAdmin(userId);
            return ResponseEntity.ok(analyticsService.registrations());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        }
    }

    @GetMapping("/tutoring-demand")
    public ResponseEntity<?> tutoringDemand(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                            @RequestParam(defaultValue = "5") int limit) {
        try {
            accessService.requireAdmin(userId);
            return ResponseEntity.ok(analyticsService.tutoringDemand(limit));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        }
    }
}
