package com.teachquest.repository;

import com.teachquest.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizId(Long quizId);
    List<QuizAttempt> findByStudentId(Long studentId);
    Optional<QuizAttempt> findByQuizIdAndStudentIdAndAttemptNumber(Long quizId, Long studentId, int attemptNumber);
    long countByQuizIdAndStudentId(Long quizId, Long studentId);
}
