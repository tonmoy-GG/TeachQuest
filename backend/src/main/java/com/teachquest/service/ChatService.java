package com.teachquest.service;

import com.teachquest.model.Message;
import com.teachquest.repository.MessageRepository;
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

    private final String UPLOAD_DIR = "uploads/";

    public Message sendMessage(Message message, MultipartFile file) throws IOException {
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
        return messageRepository.findChatHistory(userId1, userId2);
    }
}
