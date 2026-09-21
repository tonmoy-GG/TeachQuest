package com.teachquest.repository;

import com.teachquest.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByUserId(Long userId);

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM JobPost j WHERE j.userId = ?1 AND j.hiredTutorId = ?2")
    boolean existsByUserIdAndHiredTutorId(Long userId, Long hiredTutorId);

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM JobPost j JOIN com.teachquest.model.JobApplication a ON a.jobId = j.id WHERE j.userId = ?1 AND a.tutorId = ?2 AND UPPER(a.status) = 'HIRED'")
    boolean existsByUserIdAndHiredTutorIdViaApplication(Long userId, Long hiredTutorId);

    @Query("SELECT DISTINCT j.userId FROM JobPost j WHERE j.hiredTutorId = ?1")
    List<Long> findStudentIdsByHiredTutorId(Long hiredTutorId);

    @Query("SELECT DISTINCT j.userId FROM JobPost j JOIN com.teachquest.model.JobApplication a ON a.jobId = j.id WHERE a.tutorId = ?1 AND UPPER(a.status) = 'HIRED'")
    List<Long> findStudentIdsByHiredTutorIdFromApplications(Long hiredTutorId);
}
