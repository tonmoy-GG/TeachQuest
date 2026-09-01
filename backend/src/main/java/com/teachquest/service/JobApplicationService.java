package com.teachquest.service;

import com.teachquest.model.JobApplication;
import com.teachquest.model.JobPost;
import com.teachquest.repository.JobApplicationRepository;
import com.teachquest.repository.JobPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    public void applyForJob(Long jobId, Long tutorId) throws Exception {
        // Check if already applied
        if (jobApplicationRepository.findByJobIdAndTutorId(jobId, tutorId).isPresent()) {
            throw new Exception("You have already applied for this job.");
        }

        JobApplication application = new JobApplication();
        application.setJobId(jobId);
        application.setTutorId(tutorId);
        application.setAppliedAt(LocalDateTime.now());
        jobApplicationRepository.save(application);
    }

    public List<JobApplication> getApplicationsForJob(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId);
    }

    public List<JobApplication> getApplicationsByTutor(Long tutorId) {
        return jobApplicationRepository.findByTutorId(tutorId);
    }

    @Transactional
    public void hireTutor(Long jobId, Long tutorId) throws Exception {
        Optional<JobPost> jobOpt = jobPostRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            JobPost job = jobOpt.get();
            job.setHiredTutorId(tutorId);
            jobPostRepository.save(job);

            // Update application status
            Optional<JobApplication> appOpt = jobApplicationRepository.findByJobIdAndTutorId(jobId, tutorId);
            if (appOpt.isPresent()) {
                JobApplication app = appOpt.get();
                app.setStatus("HIRED");
                jobApplicationRepository.save(app);
            }
        } else {
            throw new Exception("Job not found.");
        }
    }

    @Transactional
    public void rejectApplication(Long jobId, Long tutorId) throws Exception {
        Optional<JobApplication> appOpt = jobApplicationRepository.findByJobIdAndTutorId(jobId, tutorId);
        if (appOpt.isPresent()) {
            JobApplication app = appOpt.get();
            app.setStatus("REJECTED");
            jobApplicationRepository.save(app);
        } else {
            throw new Exception("Application not found.");
        }
    }
}
