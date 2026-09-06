package com.teachquest.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_point_events", uniqueConstraints = @UniqueConstraint(columnNames = "event_key"))
public class ResourcePointEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false)
    private int points;

    @Column(name = "event_key", nullable = false, unique = true, length = 160)
    private String eventKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ResourcePointEvent() {
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
