package com.teachquest.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_flags", uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "reporter_id", "reason"}))
public class ResourceFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false, length = 30)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public ResourceFlag() {
    }

    public Long getId() { return id; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
