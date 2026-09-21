package com.teachquest.repository;

import com.teachquest.model.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {
    List<QuizAttemptAnswer> findByAttemptId(Long attemptId);
    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
