package com.teachquest.repository;

import com.teachquest.model.ResourcePointEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResourcePointEventRepository extends JpaRepository<ResourcePointEvent, Long> {
    boolean existsByEventKey(String eventKey);

    @Query("SELECT COALESCE(SUM(e.points), 0) FROM ResourcePointEvent e WHERE e.userId = :userId AND e.createdAt >= :from")
    int sumPointsSince(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    @Query("SELECT e.userId, SUM(e.points) FROM ResourcePointEvent e WHERE e.createdAt >= :from GROUP BY e.userId ORDER BY SUM(e.points) DESC")
    List<Object[]> leaderboardSince(@Param("from") LocalDateTime from);

    @Query("SELECT e.userId, SUM(e.points) FROM ResourcePointEvent e GROUP BY e.userId ORDER BY SUM(e.points) DESC")
    List<Object[]> allTimeLeaderboard();
}
