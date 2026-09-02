package com.teachquest.repository;

import com.teachquest.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query(value = "SELECT * FROM t_quiz_questions ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<QuizQuestion> findRandomQuestions(int limit);

    @Query("SELECT q FROM QuizQuestion q WHERE q.qCategory = ?1")
    List<QuizQuestion> findByQCategory(String qCategory);
}
