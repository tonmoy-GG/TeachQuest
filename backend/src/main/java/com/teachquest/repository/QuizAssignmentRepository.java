package com.teachquest.repository;

import com.teachquest.model.QuizAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Long> {
    List<QuizAssignment> findByQuizId(Long quizId);
    List<QuizAssignment> findByStudentId(Long studentId);
    boolean existsByQuizIdAndStudentId(Long quizId, Long studentId);
}
