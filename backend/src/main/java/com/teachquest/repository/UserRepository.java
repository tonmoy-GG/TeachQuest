package com.teachquest.repository;

import com.teachquest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    java.util.List<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    java.util.List<User> findByUniversityId(String universityId);
}
