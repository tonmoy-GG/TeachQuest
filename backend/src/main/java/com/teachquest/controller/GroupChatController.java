package com.teachquest.controller;

import com.teachquest.model.Message;
import com.teachquest.service.ChatService;
import com.teachquest.service.GroupChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/chat/groups")
@CrossOrigin(origins = "*")
public class GroupChatController {
    private final GroupChatService groupChatService;
    private final ChatService chatService;

    public GroupChatController(GroupChatService groupChatService, ChatService chatService) {
        this.groupChatService = groupChatService;
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<?> getGroups(@RequestParam Long userId) {
        return execute(() -> groupChatService.getGroups(userId));
    }

    @GetMapping("/eligible-students")
    public ResponseEntity<?> getEligibleStudents(@RequestParam Long tutorId) {
        return execute(() -> groupChatService.getEligibleStudents(tutorId));
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        return execute(() -> groupChatService.createGroup(request.getOwnerId(), request.getName(), request.getStudentIds()));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody MemberRequest request) {
        return execute(() -> groupChatService.addStudent(groupId, request.getRequesterId(), request.getUserId()));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @PathVariable Long userId, @RequestParam Long requesterId) {
        return execute(() -> groupChatService.removeStudent(groupId, requesterId, userId));
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leave(@PathVariable Long groupId, @RequestParam Long userId) {
        return execute(() -> { groupChatService.leaveGroup(groupId, userId); return Collections.singletonMap("ok", true); });
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long groupId, @RequestParam Long userId) {
        return execute(() -> chatService.getGroupHistory(groupId, userId));
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long groupId,
                                         @RequestParam Long senderId,
                                         @RequestParam(value = "message", required = false) String messageText,
                                         @RequestParam(value = "file", required = false) MultipartFile file) {
        return execute(() -> chatService.sendGroupMessage(groupId, senderId, messageText, file));
    }

    private ResponseEntity<?> execute(CheckedSupplier supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Group chat request failed: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier { Object get() throws Exception; }

    public static class CreateGroupRequest {
        private Long ownerId;
        private String name;
        private List<Long> studentIds;
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Long> getStudentIds() { return studentIds; }
        public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }
    }

    public static class MemberRequest {
        private Long requesterId;
        private Long userId;
        public Long getRequesterId() { return requesterId; }
        public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
