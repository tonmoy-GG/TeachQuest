package com.teachquest.repository;

import com.teachquest.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStudentId(String studentId);

    Quiz findByTitle(String title);
}
