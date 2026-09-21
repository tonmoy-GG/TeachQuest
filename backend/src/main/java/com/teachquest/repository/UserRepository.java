package com.teachquest.repository;

import com.teachquest.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmail(String email);

    List<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findByUniversityId(String universityId);

    @Query("SELECT u.userType, COUNT(u) FROM User u GROUP BY u.userType")
    List<Object[]> countByUserType();

    @Query(value = "SELECT DATE_FORMAT(DATE_SUB(DATE(created_at), INTERVAL WEEKDAY(created_at) DAY), '%Y-%m-%d') AS week_start, COUNT(*) " +
            "FROM registration WHERE created_at >= :since GROUP BY week_start ORDER BY week_start", nativeQuery = true)
    List<Object[]> registrationCountsByWeek(@Param("since") LocalDateTime since);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR LOWER(u.userType) = LOWER(:role)) " +
            "AND (:status IS NULL OR UPPER(u.status) = UPPER(:status)) " +
            "AND (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))" )
    Page<User> searchAdminUsers(@Param("role") String role,
                                @Param("status") String status,
                                @Param("search") String search,
                                Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.totalPoints = u.totalPoints + :points WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("points") int points);
}
