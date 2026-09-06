package com.teachquest.controller;

import com.teachquest.service.ResourceEngagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class ResourceModerationController {
    private final ResourceEngagementService engagementService;

    public ResourceModerationController(ResourceEngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/resource-flags")
    public ResponseEntity<?> openFlags(@RequestParam Long adminId) {
        try {
            return ResponseEntity.ok(engagementService.openFlags(adminId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/resource-flags/{flagId}/dismiss")
    public ResponseEntity<?> dismiss(@PathVariable Long flagId, @RequestParam Long adminId) {
        try {
            engagementService.dismissFlag(flagId, adminId);
            return ResponseEntity.ok(Map.of("status", "DISMISSED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<?> remove(@PathVariable Long resourceId, @RequestParam Long adminId) {
        try {
            engagementService.removeResource(resourceId, adminId);
            return ResponseEntity.ok(Map.of("status", "REMOVED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}