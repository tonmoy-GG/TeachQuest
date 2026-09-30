package com.teachquest.repository;

import com.teachquest.model.PracticeQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeQuizQuestionRepository extends JpaRepository<PracticeQuizQuestion, Long> {
    List<PracticeQuizQuestion> findByAttemptIdOrderByQuestionOrderAsc(Long attemptId);
}