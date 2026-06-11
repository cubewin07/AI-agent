package com.myapp.dto.response;

import com.myapp.model.enums.ApiFormat;
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
public class ModelResponse {
    private UUID id;
    private String name;
    private ApiFormat apiFormat;
    private String url;
    private Integer contextLimit;
    private Instant createdAt;
    private Instant updatedAt;
}
