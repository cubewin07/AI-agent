package com.myapp.service;

import com.myapp.agent.llm.ChatMessageDto;
import com.myapp.dto.request.CreateSessionRequest;
import com.myapp.dto.response.ChatMessageResponse;
import com.myapp.dto.response.SessionResponse;
import com.myapp.model.UserEntity;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    SessionResponse create(CreateSessionRequest request, UserEntity user);
    List<SessionResponse> getAll(UserEntity user);
    SessionResponse getById(UUID id, UserEntity user);
    void delete(UUID id, UserEntity user);
    
    String chat(UUID id, String query, List<ChatMessageDto> history, UserEntity user);
    List<ChatMessageResponse> getHistory(UUID id, UserEntity user);
}
