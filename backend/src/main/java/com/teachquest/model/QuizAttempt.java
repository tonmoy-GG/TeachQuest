package com.teachquest.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "quiz_id", nullable = false) private Long quizId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(nullable = false, length = 20) private String status = "IN_PROGRESS";
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "auto_score", nullable = false) private int autoScore;
    @Column(name = "manual_score", nullable = false) private int manualScore;
    @Column(name = "total_score", nullable = false) private int totalScore;

    @PrePersist protected void onCreate() { if (startedAt == null) startedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public int getAutoScore() { return autoScore; }
    public void setAutoScore(int autoScore) { this.autoScore = autoScore; }
    public int getManualScore() { return manualScore; }
    public void setManualScore(int manualScore) { this.manualScore = manualScore; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
}
