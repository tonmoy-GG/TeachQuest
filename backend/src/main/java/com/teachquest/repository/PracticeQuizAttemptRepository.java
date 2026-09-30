package com.teachquest.repository;

import com.teachquest.model.PracticeQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

public interface PracticeQuizAttemptRepository extends JpaRepository<PracticeQuizAttempt, Long> {
    Optional<PracticeQuizAttempt> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PracticeQuizAttempt> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
    List<PracticeQuizAttempt> findByUserIdAndCourseId(Long userId, Long courseId);
    List<PracticeQuizAttempt> findByUserIdAndCourseIdAndLevelOrderByCreatedAtDesc(Long userId, Long courseId, int level);
    List<PracticeQuizAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from PracticeQuizAttempt attempt where attempt.id = :attemptId and attempt.userId = :userId")
    Optional<PracticeQuizAttempt> findOwnedAttemptForUpdate(@Param("attemptId") Long attemptId, @Param("userId") Long userId);
}