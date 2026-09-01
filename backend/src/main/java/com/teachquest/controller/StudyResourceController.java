package com.teachquest.controller;

import com.teachquest.model.StudyResource;
import com.teachquest.service.StudyResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class StudyResourceController {

    @Autowired
    private StudyResourceService studyResourceService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResource(
            @RequestParam("department") String department,
            @RequestParam("category") String category,
            @RequestParam("semester") String semester,
            @RequestParam("courseCode") String courseCode,
            @RequestParam("description") String description,
            @RequestParam("uploaderId") Long uploaderId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "externalUrl", required = false) String externalUrl) {

        try {
            StudyResource resource = new StudyResource();
            resource.setDepartment(department);
            resource.setCategory(category);
            resource.setSemester(semester);
            resource.setCourseCode(courseCode);
            resource.setDescription(description);
            resource.setUploaderId(uploaderId);

            StudyResource savedResource;
            if (file != null && !file.isEmpty()) {
                resource.setFileType(file.getContentType());
                savedResource = studyResourceService.saveResource(resource, file);
            } else if (externalUrl != null && !externalUrl.trim().isEmpty()) {
                savedResource = studyResourceService.saveExternalResource(resource, externalUrl);
            } else {
                return ResponseEntity.badRequest().body("Either a file or an external URL must be provided.");
            }

            return ResponseEntity.ok("Resource uploaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error uploading resource: " + e.getMessage());
        }
    }

    @PostMapping("/upload-chunk")
    public ResponseEntity<?> uploadChunk(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks) {
        try {
            studyResourceService.saveChunk(chunk, fileName, chunkIndex, totalChunks);
            return ResponseEntity.ok("Chunk " + chunkIndex + " uploaded");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Chunk upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<StudyResource> getAllResources() {
        return studyResourceService.getAllResources();
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getDistinctCourses() {
        return ResponseEntity.ok(studyResourceService.getDistinctCourses());
    }

    @GetMapping("/filter")
    public List<StudyResource> getResourcesByFilter(
            @RequestParam String courseCode,
            @RequestParam String category) {
        return studyResourceService.getResourcesByCourseAndCategory(courseCode, category);
    }
}
