package com.teachquest.repository;

import com.teachquest.model.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {
    List<ChatGroupMember> findByGroupId(Long groupId);
    List<ChatGroupMember> findByUserId(Long userId);
    Optional<ChatGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}
