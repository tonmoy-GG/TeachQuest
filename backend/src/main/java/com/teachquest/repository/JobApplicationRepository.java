package com.teachquest.repository;

import com.teachquest.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByTutorId(Long tutorId);

    Optional<JobApplication> findByJobIdAndTutorId(Long jobId, Long tutorId);
}
