package com.myapp.service;

import com.myapp.dto.request.CreateExampleRequest;
import com.myapp.dto.response.ExampleResponse;
import com.myapp.exception.ResourceNotFoundException;
import com.myapp.model.ExampleEntity;
import com.myapp.model.enums.ExampleEnum;
import com.myapp.repository.ExampleRepository;
import com.myapp.service.impl.ExampleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExampleServiceTest {

    @Mock
    private ExampleRepository exampleRepository;

    @InjectMocks
    private ExampleServiceImpl exampleService;

    @Test
    void create_shouldPersistAndReturnResponse() {
        CreateExampleRequest request = CreateExampleRequest.builder()
                .name("Test")
                .description("Desc")
                .status(ExampleEnum.ACTIVE)
                .build();

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ExampleEntity saved = ExampleEntity.builder()
                .id(id)
                .name("Test")
                .description("Desc")
                .status(ExampleEnum.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(exampleRepository.save(any(ExampleEntity.class))).thenReturn(saved);

        ExampleResponse response = exampleService.create(request);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Test");
        verify(exampleRepository).save(any(ExampleEntity.class));
    }

    @Test
    void getById_whenNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(exampleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exampleService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
