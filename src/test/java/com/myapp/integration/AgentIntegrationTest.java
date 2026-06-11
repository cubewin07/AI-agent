package com.myapp.integration;

import com.myapp.agent.llm.LlmClient;
import com.myapp.agent.llm.LlmClientFactory;
import com.myapp.dto.request.ChatRequest;
import com.myapp.dto.request.CreateModelRequest;
import com.myapp.dto.request.CreateSessionRequest;
import com.myapp.dto.response.ChatMessageResponse;
import com.myapp.dto.response.ModelResponse;
import com.myapp.dto.response.SessionResponse;
import com.myapp.model.enums.ApiFormat;
import com.myapp.security.dto.AuthResponse;
import com.myapp.security.dto.LoginRequest;
import com.myapp.security.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private LlmClientFactory llmClientFactory;

    private final LlmClient llmClient = org.mockito.Mockito.mock(LlmClient.class);

    private String accessToken;

    @BeforeEach
    void authenticate() {
        String email = "user-" + System.nanoTime() + "@example.com";
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password("password123")
                .build();

        restTemplate.postForEntity(url("/api/v1/auth/register"), registerRequest, AuthResponse.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("password123")
                .build();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                loginRequest,
                AuthResponse.class
        );

        accessToken = loginResponse.getBody().getAccessToken();
    }

    @Test
    void shouldManageModelsSessionsAndChat() {
        HttpHeaders headers = authHeaders();

        // 1. Create Model
        CreateModelRequest modelRequest = CreateModelRequest.builder()
                .name("gpt-4")
                .apiFormat(ApiFormat.OPENAI)
                .apiKey("test-key")
                .url("https://api.openai.com/v1/chat/completions")
                .contextLimit(8192)
                .build();

        ResponseEntity<ModelResponse> createModelResponse = restTemplate.exchange(
                url("/api/v1/models"),
                HttpMethod.POST,
                new HttpEntity<>(modelRequest, headers),
                ModelResponse.class
        );

        assertThat(createModelResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createModelResponse.getBody()).isNotNull();
        UUID modelId = createModelResponse.getBody().getId();
        assertThat(createModelResponse.getBody().getName()).isEqualTo("gpt-4");

        // 2. Create Session
        CreateSessionRequest sessionRequest = CreateSessionRequest.builder()
                .name("My Chat Session")
                .modelId(modelId)
                .systemPrompt("You are a helpful assistant.")
                .enabledTools("calculator")
                .build();

        ResponseEntity<SessionResponse> createSessionResponse = restTemplate.exchange(
                url("/api/v1/sessions"),
                HttpMethod.POST,
                new HttpEntity<>(sessionRequest, headers),
                SessionResponse.class
        );

        assertThat(createSessionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createSessionResponse.getBody()).isNotNull();
        UUID sessionId = createSessionResponse.getBody().getId();
        assertThat(createSessionResponse.getBody().getName()).isEqualTo("My Chat Session");

        // 3. Mock LLM Client for Chat
        when(llmClientFactory.getClient(ApiFormat.OPENAI)).thenReturn(llmClient);
        when(llmClient.generate(any(), any(), any(), any(), any()))
                .thenReturn("Thought: The user is greeting me.\nFinal Answer: Hello! How can I help you today?");

        // 4. Send Chat Query
        ChatRequest chatRequest = ChatRequest.builder()
                .query("Hello")
                .build();

        ResponseEntity<String> chatResponse = restTemplate.exchange(
                url("/api/v1/sessions/" + sessionId + "/chat"),
                HttpMethod.POST,
                new HttpEntity<>(chatRequest, headers),
                String.class
        );

        assertThat(chatResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chatResponse.getBody()).isEqualTo("Hello! How can I help you today?");

        // 5. Get Chat History
        ResponseEntity<ChatMessageResponse[]> historyResponse = restTemplate.exchange(
                url("/api/v1/sessions/" + sessionId + "/history"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatMessageResponse[].class
        );

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody().length).isEqualTo(2);
        assertThat(historyResponse.getBody()[0].getContent()).isEqualTo("Hello");
        assertThat(historyResponse.getBody()[1].getContent()).isEqualTo("Hello! How can I help you today?");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
