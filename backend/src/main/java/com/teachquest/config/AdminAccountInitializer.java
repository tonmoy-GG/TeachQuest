package com.teachquest.config;

import com.teachquest.model.User;
import com.teachquest.repository.UserRepository;
import com.teachquest.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    public AdminAccountInitializer(UserRepository userRepository, JdbcTemplate jdbcTemplate, UserService userService) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @Value("${teachquest.admin.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${teachquest.admin.username:System Admin}")
    private String username;

    @Value("${teachquest.admin.university-id:ADMIN001}")
    private String universityId;

    @Value("${teachquest.admin.contact-no:0000000000}")
    private String contactNo;

    @Value("${teachquest.admin.email:admin@teachquest.com}")
    private String email;

    @Value("${teachquest.admin.password:admin123}")
    private String password;

    @Override
    public void run(String... args) {
        widenUserTypeColumnIfNeeded();
        if (seedEnabled && userRepository.findByEmailIgnoreCase(email).isEmpty()) {
            User admin = new User();
            admin.setUsername(username);
            admin.setUniversityId(universityId);
            admin.setContactNo(contactNo);
            admin.setPassword(password);
            admin.setEmail(email);
            admin.setAddress("TeachQuest administration");
            admin.setUserType("admin");
            admin.setStatus("ACTIVE");
            userRepository.save(admin);
        }

        userService.seedDemoAccountsIfMissing();
    }

    private void widenUserTypeColumnIfNeeded() {
        jdbcTemplate.execute("ALTER TABLE registration MODIFY COLUMN user_type VARCHAR(20) NOT NULL");
    }
}
