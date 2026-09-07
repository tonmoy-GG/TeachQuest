package com.teachquest.repository;

import com.teachquest.model.ResourceFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceFlagRepository extends JpaRepository<ResourceFlag, Long> {
    List<ResourceFlag> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByResourceIdAndReporterIdAndReason(Long resourceId, Long reporterId, String reason);
}
