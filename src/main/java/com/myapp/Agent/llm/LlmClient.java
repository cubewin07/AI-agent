package com.myapp.agent.llm;

import java.util.List;

public interface LlmClient {
    String generate(String systemPrompt, List<ChatMessageDto> history, String apiKey, String url, String modelName);
}
