package com.teachquest.controller;

import com.teachquest.model.User;
import com.teachquest.service.UserService;
import com.teachquest.service.ResourceEngagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceEngagementService resourceEngagementService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<java.util.List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}/points")
    public ResponseEntity<?> getUserPoints(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(java.util.Map.of("userId", user.getId(), "totalPoints", user.getTotalPoints())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/resource-points")
    public ResponseEntity<?> getResourceSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(resourceEngagementService.contributionSummary(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
