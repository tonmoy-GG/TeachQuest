package com.teachquest.repository;

import com.teachquest.model.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
	java.util.List<ChatGroup> findByOwnerId(Long ownerId);
}
