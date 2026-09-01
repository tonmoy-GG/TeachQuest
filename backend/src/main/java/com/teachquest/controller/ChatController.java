package com.teachquest.controller;

import com.teachquest.model.Message;
import com.teachquest.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestParam("senderId") Long senderId,
            @RequestParam("receiverId") Long receiverId,
            @RequestParam(value = "message", required = false) String messageText,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            Message message = new Message();
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setMessage(messageText != null ? messageText : "");

            Message sentMessage = chatService.sendMessage(message, file);
            return ResponseEntity.ok(sentMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending message: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public List<Message> getChatHistory(@RequestParam Long userId1, @RequestParam Long userId2) {
        return chatService.getChatHistory(userId1, userId2);
    }
}
