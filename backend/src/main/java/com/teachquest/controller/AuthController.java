package com.teachquest.controller;

import com.teachquest.model.User;
import com.teachquest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow requests from any origin for now (e.g. if we have a separate frontend)
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok("Account created successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            System.err.println("DEBUG AUTH: Incoming Login Request");
            System.err.println("DEBUG AUTH: User Email: " + user.getEmail());
            System.err.println("DEBUG AUTH: User UniId: " + user.getUniversityId());
            System.err.println("DEBUG AUTH: User Name:  " + user.getUsername());

            String identifier = user.getEmail();
            if (identifier == null || identifier.isEmpty()) {
                identifier = user.getUniversityId();
            }
            if (identifier == null || identifier.isEmpty()) {
                identifier = user.getUsername();
            }

            System.err.println("DEBUG AUTH: Derived Identifier: " + identifier);

            User loggedInUser = userService.loginUser(identifier, user.getPassword());
            return ResponseEntity.ok(loggedInUser);
        } catch (Exception e) {
            System.err.println("DEBUG AUTH: Login Error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
