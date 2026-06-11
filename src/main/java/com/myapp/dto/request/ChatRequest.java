package com.myapp.dto.request;

import com.myapp.agent.llm.ChatMessageDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private List<ChatMessageDto> history;
}
