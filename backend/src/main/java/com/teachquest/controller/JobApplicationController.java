package com.teachquest.controller;

import com.teachquest.model.JobApplication;
import com.teachquest.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class JobApplicationController {

    @Autowired
    private JobApplicationService jobApplicationService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(@RequestParam Long jobId, @RequestParam Long tutorId) {
        try {
            jobApplicationService.applyForJob(jobId, tutorId);
            return ResponseEntity.ok("Application submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}")
    public List<JobApplication> getApplications(@PathVariable Long jobId) {
        return jobApplicationService.getApplicationsForJob(jobId);
    }

    @GetMapping("/my-applications/{tutorId}")
    public List<JobApplication> getMyApplications(@PathVariable Long tutorId) {
        return jobApplicationService.getApplicationsByTutor(tutorId);
    }

    @PostMapping("/hire")
    public ResponseEntity<?> hireTutor(@RequestParam Long jobId, @RequestParam Long tutorId) {
        try {
            jobApplicationService.hireTutor(jobId, tutorId);
            return ResponseEntity.ok("Tutor hired successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectApplication(@RequestParam Long jobId, @RequestParam Long tutorId) {
        try {
            jobApplicationService.rejectApplication(jobId, tutorId);
            return ResponseEntity.ok("Application rejected successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
