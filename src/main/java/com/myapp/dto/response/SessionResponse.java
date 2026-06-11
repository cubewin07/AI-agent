package com.myapp.dto.response;

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
public class SessionResponse {
    private UUID id;
    private String name;
    private String systemPrompt;
    private UUID modelId;
    private String modelName;
    private String enabledTools;
    private Instant createdAt;
    private Instant updatedAt;
}
