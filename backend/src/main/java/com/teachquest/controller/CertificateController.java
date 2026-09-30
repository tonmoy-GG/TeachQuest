package com.teachquest.controller;

import com.teachquest.service.CourseCertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {
    private final CourseCertificateService certificateService;

    public CertificateController(CourseCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public ResponseEntity<?> certificates(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(certificateService.certificatesForUser(userId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<?> verify(@PathVariable String certificateId) {
        try {
            return ResponseEntity.ok(certificateService.verify(certificateId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404).body("Not found");
        }
    }

    @GetMapping("/{certificateId}/pdf")
    public ResponseEntity<?> pdf(@PathVariable String certificateId, @RequestParam Long userId) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teachquest-" + certificateId + ".pdf")
                    .body(certificateService.pdf(certificateId, userId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404).body(exception.getMessage());
        }
    }

    @GetMapping("/{certificateId}/social-card")
    public ResponseEntity<?> socialCard(@PathVariable String certificateId, @RequestParam Long userId) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teachquest-" + certificateId + ".png")
                    .body(certificateService.socialCard(certificateId, userId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404).body(exception.getMessage());
        }
    }
}