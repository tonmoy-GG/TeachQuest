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
@Table(name = "practice_quiz_level_progress", uniqueConstraints =
        @UniqueConstraint(name = "uq_practice_progress_user_course", columnNames = {"user_id", "course_id"}))
public class QuizLevelProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "highest_unlocked_level", nullable = false)
    private int highestUnlockedLevel = 1;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @javax.persistence.PrePersist
    @javax.persistence.PreUpdate
    protected void onChange() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public int getHighestUnlockedLevel() { return highestUnlockedLevel; }
    public void setHighestUnlockedLevel(int highestUnlockedLevel) { this.highestUnlockedLevel = highestUnlockedLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}