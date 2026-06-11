package com.myapp.agent.llm;

import com.myapp.model.enums.ApiFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LlmClientFactory {

    private final OpenAiLlmClient openAiLlmClient;
    private final AnthropicLlmClient anthropicLlmClient;
    private final GeminiLlmClient geminiLlmClient;

    public LlmClient getClient(ApiFormat format) {
        return switch (format) {
            case OPENAI -> openAiLlmClient;
            case ANTHROPIC -> anthropicLlmClient;
            case GEMINI -> geminiLlmClient;
        };
    }
}
