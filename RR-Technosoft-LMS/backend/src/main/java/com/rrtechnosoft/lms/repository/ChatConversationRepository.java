package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    List<ChatConversation> findByStudentIdOrderByUpdatedAtDesc(UUID studentId);

    Optional<ChatConversation> findByIdAndStudentId(UUID id, UUID studentId);
}
