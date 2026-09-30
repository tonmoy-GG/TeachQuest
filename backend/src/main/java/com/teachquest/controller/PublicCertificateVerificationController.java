package com.teachquest.controller;

import com.teachquest.service.CourseCertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class PublicCertificateVerificationController {
    private final CourseCertificateService certificateService;

    public PublicCertificateVerificationController(CourseCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<?> verify(@PathVariable String certificateId) {
        try {
            return ResponseEntity.ok(certificateService.verify(certificateId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404).body("Not found");
        }
    }
}