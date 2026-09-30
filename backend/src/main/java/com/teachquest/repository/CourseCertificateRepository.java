package com.teachquest.repository;

import com.teachquest.model.CourseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCertificateRepository extends JpaRepository<CourseCertificate, Long> {
    Optional<CourseCertificate> findByCertificateId(String certificateId);
    Optional<CourseCertificate> findFirstByUserIdAndCourseIdAndStatusOrderByIssuedAtDesc(
            Long userId, Long courseId, String status);
    List<CourseCertificate> findByUserIdOrderByIssuedAtDesc(Long userId);
}