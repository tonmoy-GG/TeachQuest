package com.teachquest.repository;

import com.teachquest.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.tutorId = :tutorId")
    Double averageScoreForTutor(@Param("tutorId") Long tutorId);
}
