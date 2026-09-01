package com.teachquest.service;

import com.teachquest.model.StudyResource;
import com.teachquest.repository.StudyResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final String UPLOAD_DIR = "uploads/";

    public StudyResource saveResource(StudyResource resource, MultipartFile file) throws IOException {
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
            return studyResourceRepository.save(resource);
        } catch (IOException e) {
            throw new IOException("Could not store file " + fileName + ". Please try again!", e);
        }
    }

    public StudyResource saveExternalResource(StudyResource resource, String externalUrl) {
        resource.setFilePath(externalUrl);
        resource.setExternal(true);
        resource.setFileType("url/link");
        return studyResourceRepository.save(resource);
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
        return studyResourceRepository.findAll();
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
