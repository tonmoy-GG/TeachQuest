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
        if (user == null) {
            throw new Exception("User details are required.");
        }

        String email = normalizeEmail(user.getEmail());
        String username = normalizeUsername(user.getUsername());
        if (email == null || email.isBlank()) {
            throw new Exception("Email is required.");
        }
        if (username == null || username.isBlank()) {
            throw new Exception("Username is required.");
        }

        if (!userRepository.findByEmailIgnoreCase(email).isEmpty()) {
            throw new Exception("Email already exists!");
        }

        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new Exception("Username already exists!");
        }

        String normalizedUserType = normalizeUserType(user.getUserType());
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(user.getPassword() == null ? "" : user.getPassword().trim());
        user.setAddress(user.getAddress() == null ? "" : user.getAddress().trim());
        user.setUserType(normalizedUserType);
        user.setStatus("ACTIVE");

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
            if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
                throw new Exception("This account has been suspended.");
            }
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

    public void seedDemoAccountsIfMissing() {
        createDemoUserIfMissing("student1@gmail.com", "student1", "student", "student123", "STUDENT001", "01700000001", "Demo Student Street");
        createDemoUserIfMissing("teacher1@gmail.com", "teacher1", "teacher", "teacher123", "TEACHER001", "01700000002", "Demo Teacher Street");
    }

    private void createDemoUserIfMissing(String email, String username, String userType, String password, String universityId, String contactNo, String address) {
        if (userRepository.findByEmailIgnoreCase(email).isEmpty() && userRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setUserType(userType);
            user.setPassword(password);
            user.setUniversityId(universityId);
            user.setContactNo(contactNo);
            user.setAddress(address);
            user.setStatus("ACTIVE");
            userRepository.save(user);
        }
    }

    private String normalizeEmail(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase();
    }

    private String normalizeUsername(String value) {
        if (value == null) return null;
        return value.trim();
    }

    private String normalizeUserType(String value) {
        if (value == null || value.isBlank()) return "student";
        String normalized = value.trim().toLowerCase();
        if ("tutor".equals(normalized) || "teacher".equals(normalized)) return "teacher";
        if ("job_poster".equals(normalized) || "jobposter".equals(normalized) || "job poster".equals(normalized)) return "jobposter";
        if ("admin".equals(normalized)) return "student";
        return "student";
    }
}
