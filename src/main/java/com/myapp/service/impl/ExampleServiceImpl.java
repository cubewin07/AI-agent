package com.myapp.service.impl;

import com.myapp.dto.request.CreateExampleRequest;
import com.myapp.dto.response.ExampleResponse;
import com.myapp.exception.ResourceNotFoundException;
import com.myapp.model.ExampleEntity;
import com.myapp.repository.ExampleRepository;
import com.myapp.service.ExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository exampleRepository;

    @Override
    @Transactional
    @CacheEvict(value = "examples", allEntries = true)
    public ExampleResponse create(CreateExampleRequest request) {
        ExampleEntity entity = ExampleEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();

        ExampleEntity saved = exampleRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Cacheable(value = "examples", key = "#id")
    public ExampleResponse getById(UUID id) {
        ExampleEntity entity = exampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Example not found with id: " + id));
        return toResponse(entity);
    }

    @Override
    public List<ExampleResponse> getAll() {
        return exampleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ExampleResponse toResponse(ExampleEntity entity) {
        return ExampleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
