package com.teachquest.service;

import com.teachquest.model.User;
import com.teachquest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User registerUser(User user) throws Exception {
        // Check if email already exists
        if (!userRepository.findByEmail(user.getEmail()).isEmpty()) {
            throw new Exception("Email already exists!");
        }

        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new Exception("Username already exists!");
        }

        // In a real application, you should hash the password here.
        // For now, we'll keep it simple as we don't have Spring Security configured
        // yet.
        // user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Ensure address is not null if it wasn't provided (though DB allows it)
        if (user.getAddress() == null) {
            user.setAddress("");
        }

        return userRepository.save(user);
    }

    public User loginUser(String identifier, String password) throws Exception {
        System.err.println("DEBUG LOGIN START: Identifier=[" + identifier + "]");

        // Try to find by email first
        java.util.List<User> usersByEmail = userRepository.findByEmail(identifier);

        // If not found by email, try by University ID
        if (usersByEmail.isEmpty()) {
            usersByEmail = userRepository.findByUniversityId(identifier);
        }

        if (usersByEmail.isEmpty()) {
            System.err.println("DEBUG LOGIN: No user found for identifier: " + identifier);
            throw new Exception("User not found!");
        }

        System.err.println("DEBUG LOGIN: Found " + usersByEmail.size() + " candidates. Checking passwords...");

        for (User user : usersByEmail) {
            String storedPass = (user.getPassword() != null) ? user.getPassword().trim() : "";
            String providedPass = (password != null) ? password.trim() : "";

            System.err.println(
                    "DEBUG LOGIN: Comparing candidate [" + user.getUsername() + "] role [" + user.getUserType() + "]");

            if (storedPass.equals(providedPass)) {
                System.err.println("DEBUG LOGIN: Match found for user: " + user.getUsername());
                return user;
            }
        }

        System.err.println("DEBUG LOGIN: No password match among candidates.");
        throw new Exception("Invalid password!");
    }

    public java.util.Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
