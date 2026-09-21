package com.teachquest.repository;

import com.teachquest.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByUserId(Long userId);

    boolean existsByUserIdAndHiredTutorId(Long userId, Long hiredTutorId);

    @Query("SELECT DISTINCT j.userId FROM JobPost j WHERE j.hiredTutorId = ?1")
    List<Long> findStudentIdsByHiredTutorId(Long hiredTutorId);
}
package com.teachquest.repository;

import com.teachquest.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByUserId(Long userId);
}
