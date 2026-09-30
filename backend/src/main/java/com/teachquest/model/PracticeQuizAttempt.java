package com.teachquest.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_quiz_attempts")
public class PracticeQuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_id", nullable = false)
    private Long seriesId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private int level;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "pass_threshold_percent", nullable = false)
    private double passThresholdPercent;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "score_percent")
    private Double scorePercent;

    @Column
    private Boolean passed;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @javax.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSeriesId() { return seriesId; }
    public void setSeriesId(Long seriesId) { this.seriesId = seriesId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public double getPassThresholdPercent() { return passThresholdPercent; }
    public void setPassThresholdPercent(double passThresholdPercent) { this.passThresholdPercent = passThresholdPercent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getScorePercent() { return scorePercent; }
    public void setScorePercent(Double scorePercent) { this.scorePercent = scorePercent; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}