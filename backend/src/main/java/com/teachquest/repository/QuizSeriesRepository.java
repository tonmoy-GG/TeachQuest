package com.teachquest.repository;

import com.teachquest.model.QuizSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSeriesRepository extends JpaRepository<QuizSeries, Long> {
    Optional<QuizSeries> findByUserIdAndCourseIdAndTopicIgnoreCase(Long userId, Long courseId, String topic);
}