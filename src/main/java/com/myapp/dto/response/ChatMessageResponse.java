package com.myapp.dto.response;

import com.myapp.model.enums.ChatRole;
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
public class ChatMessageResponse {
    private UUID id;
    private ChatRole role;
    private String content;
    private String toolName;
    private Integer stepNumber;
    private Instant createdAt;
}
