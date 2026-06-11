package com.myapp.service.impl;

import com.myapp.agent.Agent;
import com.myapp.dto.request.CreateSessionRequest;
import com.myapp.dto.response.ChatMessageResponse;
import com.myapp.dto.response.SessionResponse;
import com.myapp.exception.ResourceNotFoundException;
import com.myapp.model.ChatMessageEntity;
import com.myapp.model.ModelEntity;
import com.myapp.model.SessionEntity;
import com.myapp.model.UserEntity;
import com.myapp.repository.ChatMessageRepository;
import com.myapp.repository.ModelRepository;
import com.myapp.repository.SessionRepository;
import com.myapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final ModelRepository modelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Agent agent;

    @Override
    @Transactional
    public SessionResponse create(CreateSessionRequest request, UserEntity user) {
        ModelEntity model = modelRepository.findByIdAndOwner(request.getModelId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Model not found with id: " + request.getModelId()));

        SessionEntity entity = SessionEntity.builder()
                .name(request.getName())
                .systemPrompt(request.getSystemPrompt())
                .model(model)
                .owner(user)
                .enabledTools(request.getEnabledTools())
                .build();

        SessionEntity saved = sessionRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    public List<SessionResponse> getAll(UserEntity user) {
        return sessionRepository.findByOwner(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SessionResponse getById(UUID id, UserEntity user) {
        SessionEntity entity = sessionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id, UserEntity user) {
        SessionEntity entity = sessionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        sessionRepository.delete(entity);
    }

    @Override
    @Transactional
    public String chat(UUID id, String query, UserEntity user) {
        SessionEntity session = sessionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        return agent.processQuery(session, query);
    }

    @Override
    public List<ChatMessageResponse> getHistory(UUID id, UserEntity user) {
        SessionEntity session = sessionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private SessionResponse toResponse(SessionEntity entity) {
        return SessionResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .systemPrompt(entity.getSystemPrompt())
                .modelId(entity.getModel().getId())
                .modelName(entity.getModel().getName())
                .enabledTools(entity.getEnabledTools())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessageEntity entity) {
        return ChatMessageResponse.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .toolName(entity.getToolName())
                .stepNumber(entity.getStepNumber())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
