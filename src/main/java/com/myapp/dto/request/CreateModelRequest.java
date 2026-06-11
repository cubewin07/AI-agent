package com.myapp.dto.request;

import com.myapp.model.enums.ApiFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateModelRequest {

    @NotBlank(message = "Model name is required")
    private String name;

    @NotNull(message = "API format is required")
    private ApiFormat apiFormat;

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "URL is required")
    private String url;

    private Integer contextLimit;
}
