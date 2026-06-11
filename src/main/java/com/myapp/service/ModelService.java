package com.myapp.service;

import com.myapp.dto.request.CreateModelRequest;
import com.myapp.dto.response.ModelResponse;
import com.myapp.model.UserEntity;

import java.util.List;
import java.util.UUID;

public interface ModelService {
    ModelResponse create(CreateModelRequest request, UserEntity user);
    List<ModelResponse> getAll(UserEntity user);
    ModelResponse getById(UUID id, UserEntity user);
    void delete(UUID id, UserEntity user);
}
