package com.teachquest.repository;

import com.teachquest.model.TutoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TutoringSessionRepository extends JpaRepository<TutoringSession, Long> {
    long countByStatusAndCompletedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT s.tutorId) FROM TutoringSession s " +
            "WHERE s.status = 'COMPLETED' AND s.completedAt >= :since")
    long countActiveTutorsSince(@Param("since") LocalDateTime since);

    List<TutoringSession> findByTutorIdOrderByRequestedAtDesc(Long tutorId);
    List<TutoringSession> findByStudentIdOrderByRequestedAtDesc(Long studentId);

    @Modifying
    @Query("UPDATE TutoringSession s SET s.status = 'CANCELLED', s.cancelledAt = :now " +
            "WHERE (s.studentId = :userId OR s.tutorId = :userId) " +
            "AND s.status IN ('REQUESTED', 'ACCEPTED')")
    int cancelPendingForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query(value = "SELECT DATE_FORMAT(DATE_SUB(DATE(requested_at), INTERVAL WEEKDAY(requested_at) DAY), '%Y-%m-%d') AS week_start, COUNT(*) AS requests " +
            "FROM tutoring_sessions WHERE requested_at >= :since GROUP BY week_start ORDER BY week_start", nativeQuery = true)
    List<Object[]> registrationDemandByWeek(@Param("since") LocalDateTime since);

    @Query(value = "SELECT course_code, COUNT(*) AS requests FROM tutoring_sessions " +
            "WHERE status <> 'CANCELLED' GROUP BY course_code ORDER BY requests DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topDemandByCourse(@Param("limit") int limit);
}
