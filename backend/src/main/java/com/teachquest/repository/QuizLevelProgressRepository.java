package com.teachquest.repository;

import com.teachquest.model.QuizLevelProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;

public interface QuizLevelProgressRepository extends JpaRepository<QuizLevelProgress, Long> {
    Optional<QuizLevelProgress> findByUserIdAndCourseId(Long userId, Long courseId);
    List<QuizLevelProgress> findByUserIdOrderByCourseIdAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select progress from QuizLevelProgress progress where progress.userId = :userId and progress.courseId = :courseId")
    Optional<QuizLevelProgress> findByUserIdAndCourseIdForUpdate(
            @Param("userId") Long userId, @Param("courseId") Long courseId);
}