package com.myapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotBlank(message = "Session name is required")
    private String name;

    private String systemPrompt;

    @NotNull(message = "Model ID is required")
    private UUID modelId;

    private String enabledTools; // Comma-separated list of tool names
}
