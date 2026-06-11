package com.myapp.agent.llm;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicLlmClient implements LlmClient {

    private final RestClient restClient = RestClient.builder().build();

    @Override
    @SuppressWarnings("unchecked")
    public String generate(String systemPrompt, List<ChatMessageDto> history, String apiKey, String url, String modelName) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessageDto msg : history) {
            // Anthropic only supports 'user' and 'assistant' roles in messages
            String role = msg.getRole().toLowerCase();
            if ("system".equals(role)) {
                continue; // System prompt is passed separately
            }
            if ("tool".equals(role)) {
                role = "user"; // Map tool responses to user role for simplicity in text-based ReAct
            }
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("max_tokens", 4096);
        requestBody.put("messages", messages);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            requestBody.put("system", systemPrompt);
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
                if (!contentList.isEmpty()) {
                    Map<String, Object> content = contentList.get(0);
                    if ("text".equals(content.get("type"))) {
                        return (String) content.get("text");
                    }
                }
            }
            throw new RuntimeException("Invalid response from Anthropic API: " + response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Anthropic API: " + e.getMessage(), e);
        }
    }
}
