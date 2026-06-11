package com.myapp.repository;

import com.myapp.model.ChatMessageEntity;
import com.myapp.model.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findBySessionOrderByCreatedAtAsc(SessionEntity session);
    void deleteBySession(SessionEntity session);
}
