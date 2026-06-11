package com.myapp.service.impl;

import com.myapp.dto.request.CreateModelRequest;
import com.myapp.dto.response.ModelResponse;
import com.myapp.exception.ResourceNotFoundException;
import com.myapp.model.ModelEntity;
import com.myapp.model.UserEntity;
import com.myapp.repository.ModelRepository;
import com.myapp.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;

    @Override
    @Transactional
    public ModelResponse create(CreateModelRequest request, UserEntity user) {
        ModelEntity entity = ModelEntity.builder()
                .name(request.getName())
                .apiFormat(request.getApiFormat())
                .apiKey(request.getApiKey())
                .url(request.getUrl())
                .contextLimit(request.getContextLimit())
                .owner(user)
                .build();

        ModelEntity saved = modelRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    public List<ModelResponse> getAll(UserEntity user) {
        return modelRepository.findByOwner(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ModelResponse getById(UUID id, UserEntity user) {
        ModelEntity entity = modelRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Model not found with id: " + id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id, UserEntity user) {
        ModelEntity entity = modelRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Model not found with id: " + id));
        modelRepository.delete(entity);
    }

    private ModelResponse toResponse(ModelEntity entity) {
        return ModelResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .apiFormat(entity.getApiFormat())
                .url(entity.getUrl())
                .contextLimit(entity.getContextLimit())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
