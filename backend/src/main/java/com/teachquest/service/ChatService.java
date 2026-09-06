package com.teachquest.service;

import com.teachquest.model.Message;
import com.teachquest.repository.MessageRepository;
import com.teachquest.repository.JobPostRepository;
import com.teachquest.repository.UserRepository;
import com.teachquest.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    private final String UPLOAD_DIR = "uploads/";

    public Message sendMessage(Message message, MultipartFile file) throws IOException {
        validateChatAccess(message.getSenderId(), message.getReceiverId());

        if (file != null && !file.isEmpty()) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            // Basic unique naming using timestamp
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try {
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                message.setFilePath(UPLOAD_DIR + uniqueFileName);
                message.setMessage("[File Attachment]"); // As per PHP logic
            } catch (IOException e) {
                throw new IOException("Could not store file " + fileName, e);
            }
        }

        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(Long userId1, Long userId2) {
        validateChatAccess(userId1, userId2);
        return messageRepository.findChatHistory(userId1, userId2);
    }

    private void validateChatAccess(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            throw new IllegalArgumentException("A valid student-teacher pair is required.");
        }

        User first = userRepository.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("Sender account was not found."));
        User second = userRepository.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("Receiver account was not found."));

        Long studentId;
        Long teacherId;
        if ("teacher".equalsIgnoreCase(first.getUserType()) && "student".equalsIgnoreCase(second.getUserType())) {
            teacherId = first.getId();
            studentId = second.getId();
        } else if ("student".equalsIgnoreCase(first.getUserType()) && "teacher".equalsIgnoreCase(second.getUserType())) {
            studentId = first.getId();
            teacherId = second.getId();
        } else {
            throw new IllegalArgumentException("Chat is only available between a student and a teacher.");
        }

        if (!jobPostRepository.existsByUserIdAndHiredTutorId(studentId, teacherId)) {
            throw new IllegalArgumentException("Chat is available only after a tutor has been hired.");
        }
    }
}
