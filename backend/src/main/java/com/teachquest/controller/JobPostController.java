package com.teachquest.controller;

import com.teachquest.model.JobPost;
import com.teachquest.service.JobPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobPostController {

    @Autowired
    private JobPostService jobPostService;

    @PostMapping("/create")
    public ResponseEntity<JobPost> createJobPost(@RequestBody JobPost jobPost) {
        // In a real app, we would set the userId from the authenticated user context
        // For now, we assume userId is passed in the body or handled by caller
        JobPost createdJob = jobPostService.createJobPost(jobPost);
        return ResponseEntity.ok(createdJob);
    }

    @GetMapping("/all")
    public List<JobPost> getAllJobPosts() {
        return jobPostService.getAllJobPosts();
    }

    @GetMapping("/my-jobs/{userId}")
    public List<JobPost> getMyJobPosts(@PathVariable Long userId) {
        return jobPostService.getJobPostsByUserId(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPost> getJobPostById(@PathVariable Long id) {
        return jobPostService.getJobPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteJobPost(@PathVariable Long id) {
        jobPostService.deleteJobPost(id);
        return ResponseEntity.ok("Job deleted successfully");
    }
}
