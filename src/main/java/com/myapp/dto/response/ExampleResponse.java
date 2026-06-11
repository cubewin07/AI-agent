package com.myapp.dto.response;

import com.myapp.model.enums.ExampleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleResponse {

    private UUID id;
    private String name;
    private String description;
    private ExampleEnum status;
    private Instant createdAt;
    private Instant updatedAt;
}
