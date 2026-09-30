package com.teachquest.service;

import com.teachquest.model.User;
import com.teachquest.repository.JobPostRepository;
import com.teachquest.repository.MessageRepository;
import com.teachquest.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock private MessageRepository messageRepository;
    @Mock private JobPostRepository jobPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupChatService groupChatService;
    @InjectMocks private ChatService chatService;

    @Test
    void pendingApplicantCanOpenDirectChatHistory() {
        User student = user(1L, "student");
        User tutor = user(2L, "teacher");
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(jobPostRepository.existsByUserIdAndHiredTutorId(1L, 2L)).thenReturn(false);
        when(jobPostRepository.existsByUserIdAndHiredTutorIdViaApplication(1L, 2L)).thenReturn(false);
        when(jobPostRepository.existsByUserIdAndApplicantId(1L, 2L)).thenReturn(true);
        when(messageRepository.findChatHistory(1L, 2L)).thenReturn(List.of());

        assertEquals(List.of(), chatService.getChatHistory(1L, 2L));
        verify(messageRepository).findChatHistory(1L, 2L);
    }

    @Test
    void unrelatedTutorCannotOpenDirectChatHistory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "student")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "teacher")));
        when(jobPostRepository.existsByUserIdAndHiredTutorId(1L, 2L)).thenReturn(false);
        when(jobPostRepository.existsByUserIdAndHiredTutorIdViaApplication(1L, 2L)).thenReturn(false);
        when(jobPostRepository.existsByUserIdAndApplicantId(1L, 2L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> chatService.getChatHistory(1L, 2L));
        verify(messageRepository, never()).findChatHistory(1L, 2L);
    }

    private User user(Long id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        return user;
    }
}