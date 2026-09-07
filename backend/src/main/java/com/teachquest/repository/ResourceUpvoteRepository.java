package com.teachquest.repository;

import com.teachquest.model.ResourceUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ResourceUpvoteRepository extends JpaRepository<ResourceUpvote, Long> {
    boolean existsByResourceIdAndUserId(Long resourceId, Long userId);

    @Query("SELECT COUNT(u) FROM ResourceUpvote u JOIN StudyResource r ON r.id = u.resourceId WHERE r.uploaderId = :userId")
    long countReceivedByUploader(@Param("userId") Long userId);
}
