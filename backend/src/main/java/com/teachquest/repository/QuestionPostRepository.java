package com.teachquest.repository;

import com.teachquest.model.QuestionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionPostRepository extends JpaRepository<QuestionPost, Long> {
    List<QuestionPost> findAllByOrderByCreatedAtDesc();
}
