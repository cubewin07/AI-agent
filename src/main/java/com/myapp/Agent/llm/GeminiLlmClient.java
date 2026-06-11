package com.myapp.agent.llm;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmClient implements LlmClient {

    private final RestClient restClient = RestClient.builder().build();

    @Override
    @SuppressWarnings("unchecked")
    public String generate(String systemPrompt, List<ChatMessageDto> history, String apiKey, String url, String modelName) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessageDto msg : history) {
            String role = msg.getRole().toLowerCase();
            if ("system".equals(role)) {
                continue;
            }
            // Gemini uses 'user' and 'model' roles
            if ("assistant".equals(role)) {
                role = "model";
            } else if ("tool".equals(role)) {
                role = "user";
            }
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            requestBody.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
        }

        // Build the URI
        String targetUrl = url;
        if (!targetUrl.contains("?key=") && !targetUrl.contains("&key=")) {
            if (targetUrl.contains("?")) {
                targetUrl += "&key=" + apiKey;
            } else {
                targetUrl += "?key=" + apiKey;
            }
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (!parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response from Gemini API: " + response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
}
