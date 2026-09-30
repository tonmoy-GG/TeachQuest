package com.teachquest.service;

import com.teachquest.model.ResourcePointEvent;
import com.teachquest.repository.ResourcePointEventRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CertificatePointsListener {
    private final ResourcePointEventRepository pointEventRepository;
    private final UserRepository userRepository;
    private final int bronzePoints;
    private final int silverPoints;
    private final int goldPoints;

    public CertificatePointsListener(ResourcePointEventRepository pointEventRepository,
                                     UserRepository userRepository,
                                     @Value("${teachquest.certificates.bonus-points.bronze:50}") int bronzePoints,
                                     @Value("${teachquest.certificates.bonus-points.silver:75}") int silverPoints,
                                     @Value("${teachquest.certificates.bonus-points.gold:100}") int goldPoints) {
        this.pointEventRepository = pointEventRepository;
        this.userRepository = userRepository;
        this.bronzePoints = bronzePoints;
        this.silverPoints = silverPoints;
        this.goldPoints = goldPoints;
    }

    @EventListener
    public void awardCertificatePoints(CertificateIssuedEvent event) {
        int tierPoints = switch (event.tier()) {
            case "BRONZE" -> bronzePoints;
            case "SILVER" -> silverPoints;
            case "GOLD" -> goldPoints;
            default -> 0;
        };
        int previousTierPoints = switch (event.previousTier() == null ? "" : event.previousTier()) {
            case "BRONZE" -> bronzePoints;
            case "SILVER" -> silverPoints;
            case "GOLD" -> goldPoints;
            default -> 0;
        };
        int points = Math.max(0, tierPoints - previousTierPoints);
        String eventKey = "certificate:" + event.userId() + ":" + event.courseId() + ":" + event.tier();
        if (points <= 0 || pointEventRepository.existsByEventKey(eventKey)) return;

        ResourcePointEvent pointEvent = new ResourcePointEvent();
        pointEvent.setUserId(event.userId());
        pointEvent.setEventType("CERTIFICATE");
        pointEvent.setPoints(points);
        pointEvent.setEventKey(eventKey);
        pointEvent.setCreatedAt(LocalDateTime.now());
        pointEventRepository.save(pointEvent);
        userRepository.addPoints(event.userId(), points);
    }
}