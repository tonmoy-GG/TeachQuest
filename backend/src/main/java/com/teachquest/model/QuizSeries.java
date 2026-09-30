package com.teachquest.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_quiz_series", uniqueConstraints =
        @UniqueConstraint(name = "uq_practice_series_user_course_topic", columnNames = {"user_id", "course_id", "topic"}))
public class QuizSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "question_mix", nullable = false, length = 30)
    private String questionMix;

    @Column(name = "pass_threshold_percent", nullable = false)
    private double passThresholdPercent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @javax.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getQuestionMix() { return questionMix; }
    public void setQuestionMix(String questionMix) { this.questionMix = questionMix; }
    public double getPassThresholdPercent() { return passThresholdPercent; }
    public void setPassThresholdPercent(double passThresholdPercent) { this.passThresholdPercent = passThresholdPercent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}