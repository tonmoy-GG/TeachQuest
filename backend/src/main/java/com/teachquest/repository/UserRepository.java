package com.teachquest.repository;

import com.teachquest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    java.util.List<User> findByEmail(String email);

    java.util.List<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    java.util.List<User> findByUniversityId(String universityId);

    @Modifying
    @Query("UPDATE User u SET u.totalPoints = u.totalPoints + :points WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("points") int points);
}
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
