package com.teachquest.service;

import com.teachquest.model.JobPost;
import com.teachquest.repository.JobPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobPostService {

    @Autowired
    private JobPostRepository jobPostRepository;

    public JobPost createJobPost(JobPost jobPost) {
        return jobPostRepository.save(jobPost);
    }

    public List<JobPost> getAllJobPosts() {
        return jobPostRepository.findAll();
    }

    public List<JobPost> getJobPostsByUserId(Long userId) {
        return jobPostRepository.findByUserId(userId);
    }

    public void deleteJobPost(Long id) {
        jobPostRepository.deleteById(id);
    }

    public Optional<JobPost> getJobPostById(Long id) {
        return jobPostRepository.findById(id);
    }
}
