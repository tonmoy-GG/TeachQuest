package com.teachquest.service;

import com.teachquest.model.StudyResource;
import com.teachquest.repository.StudyResourceRepository;
import com.teachquest.repository.ResourceUpvoteRepository;
import com.teachquest.repository.UserRepository;
import com.teachquest.repository.ResourcePointEventRepository;
import com.teachquest.model.ResourceUpvote;
import com.teachquest.model.User;
import com.teachquest.model.ResourcePointEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StudyResourceService {

    @Autowired
    private StudyResourceRepository studyResourceRepository;

    @Autowired
    private ResourceUpvoteRepository resourceUpvoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourcePointEventRepository resourcePointEventRepository;

    private final String UPLOAD_DIR = "uploads/";

    @Transactional
    public StudyResource saveResource(StudyResource resource, MultipartFile file) throws IOException {
        if (resource.getCreatedAt() == null) resource.setCreatedAt(java.time.LocalDateTime.now());
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Create unique filename logic as per PHP:
        // courseCode_category_semester_timestamp_filename
        String uniqueFileName = resource.getCourseCode() + "_" + resource.getCategory() + "_" + resource.getSemester()
                + "_" + System.currentTimeMillis() + "_" + fileName;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try {
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            resource.setFilePath(UPLOAD_DIR + uniqueFileName);
            resource.setExternal(false);
            StudyResource saved = studyResourceRepository.save(resource);
            awardPoints(saved.getUploaderId(), 10, "UPLOAD", saved.getId(), "upload:" + saved.getId());
            return saved;
        } catch (IOException e) {
            throw new IOException("Could not store file " + fileName + ". Please try again!", e);
        }
    }

    @Transactional
    public StudyResource saveExternalResource(StudyResource resource, String externalUrl) {
        if (resource.getCreatedAt() == null) resource.setCreatedAt(java.time.LocalDateTime.now());
        resource.setFilePath(externalUrl);
        resource.setExternal(true);
        resource.setFileType("url/link");
        StudyResource saved = studyResourceRepository.save(resource);
        awardPoints(saved.getUploaderId(), 10, "UPLOAD", saved.getId(), "upload:" + saved.getId());
        return saved;
    }

    @Transactional
    public Map<String, Object> upvoteResource(Long resourceId, Long userId) {
        User voter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Voter account was not found."));
        StudyResource resource = studyResourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource was not found."));

        if (resource.getUploaderId().equals(voter.getId())) {
            throw new IllegalArgumentException("You cannot upvote your own resource.");
        }
        if (resourceUpvoteRepository.existsByResourceIdAndUserId(resourceId, userId)) {
            throw new IllegalArgumentException("You have already upvoted this resource.");
        }

        ResourceUpvote upvote = new ResourceUpvote();
        upvote.setResourceId(resourceId);
        upvote.setUserId(userId);
        upvote.setCreatedAt(java.time.LocalDateTime.now());
        resourceUpvoteRepository.save(upvote);

        boolean pointsAwarded = resource.getUpvotePoints() < 50;
        if (pointsAwarded) {
            resource.setUpvotePoints(resource.getUpvotePoints() + 1);
            studyResourceRepository.save(resource);
            awardPoints(resource.getUploaderId(), 1, "UPVOTE_RECEIVED", resource.getId(), "upvote:" + resource.getId() + ":" + userId);
        }

        return resourcePointsResult(resource, pointsAwarded ? 1 : 0);
    }

    @Transactional
    public Map<String, Object> verifyResource(Long resourceId, Long verifierId) {
        User verifier = userRepository.findById(verifierId)
                .orElseThrow(() -> new IllegalArgumentException("Verifier account was not found."));
        String role = verifier.getUserType() == null ? "" : verifier.getUserType().trim().toLowerCase();
        if (!role.equals("teacher") && !role.equals("admin")) {
            throw new IllegalArgumentException("Only tutors or administrators can verify resources.");
        }

        StudyResource resource = studyResourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource was not found."));
        int bonusAwarded = 0;
        if (!resource.isVerifiedBonusAwarded()) {
            resource.setVerified(true);
            resource.setVerifiedBonusAwarded(true);
            studyResourceRepository.save(resource);
            awardPoints(resource.getUploaderId(), 15, "VERIFICATION", resource.getId(), "verification:" + resource.getId());
            bonusAwarded = 15;
        } else if (!resource.isVerified()) {
            resource.setVerified(true);
            studyResourceRepository.save(resource);
        }

        return resourcePointsResult(resource, bonusAwarded);
    }

    public User getUserPoints(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account was not found."));
    }

    private void awardPoints(Long userId, int points, String eventType, Long resourceId, String eventKey) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("Uploader account was not found.");
        }
        if (resourcePointEventRepository.existsByEventKey(eventKey)) return;

        ResourcePointEvent event = new ResourcePointEvent();
        event.setUserId(userId);
        event.setResourceId(resourceId);
        event.setEventType(eventType);
        event.setPoints(points);
        event.setEventKey(eventKey);
        event.setCreatedAt(java.time.LocalDateTime.now());
        resourcePointEventRepository.save(event);
        userRepository.addPoints(userId, points);
    }

    private Map<String, Object> resourcePointsResult(StudyResource resource, int pointsAwarded) {
        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", resource.getId());
        result.put("upvotePoints", resource.getUpvotePoints());
        result.put("verified", resource.isVerified());
        result.put("pointsAwarded", pointsAwarded);
        return result;
    }

    public void saveChunk(MultipartFile chunk, String fileName, int chunkIndex, int totalChunks) throws IOException {
        Path tempDir = Paths.get("uploads/temp/" + fileName);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path chunkPath = tempDir.resolve("chunk_" + chunkIndex);
        Files.copy(chunk.getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);

        // If this is the last chunk, merge them
        if (chunkIndex == totalChunks - 1) {
            Path finalPath = Paths.get(UPLOAD_DIR).resolve(fileName);
            if (!Files.exists(finalPath.getParent())) {
                Files.createDirectories(finalPath.getParent());
            }

            try (var os = Files.newOutputStream(finalPath)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path p = tempDir.resolve("chunk_" + i);
                    Files.copy(p, os);
                    Files.delete(p); // Clean up chunk
                }
            }
            Files.delete(tempDir); // Clean up temp dir
        }
    }

    public List<StudyResource> getAllResources() {
        return studyResourceRepository.findByModerationStatusOrderByCreatedAtDesc("ACTIVE");
    }

    public List<Map<String, Object>> getDistinctCourses() {
        List<Object[]> results = studyResourceRepository.findDistinctCourses();
        List<Map<String, Object>> courses = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("courseCode", row[0]);
            map.put("department", row[1]);
            map.put("semester", row[2]);
            courses.add(map);
        }
        return courses;
    }

    public List<StudyResource> getResourcesByCourseAndCategory(String courseCode, String category) {
        return studyResourceRepository.findByCourseCodeAndCategory(courseCode, category);
    }
}
