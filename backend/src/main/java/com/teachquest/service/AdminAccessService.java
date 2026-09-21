package com.teachquest.service;

import com.teachquest.model.User;
import com.teachquest.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {
    private final UserRepository userRepository;

    public AdminAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireAdmin(Long userId) {
        if (userId == null) throw new IllegalArgumentException("An authenticated admin is required.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found."));
        if (!"admin".equalsIgnoreCase(user.getUserType()) || "SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Only active administrators may access this area.");
        }
        return user;
    }
}
