package com.myapp.integration;

import com.myapp.dto.request.CreateExampleRequest;
import com.myapp.dto.response.ExampleResponse;
import com.myapp.model.enums.ExampleEnum;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExampleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
    void shouldCreateAndRetrieveExample() {
        CreateExampleRequest request = CreateExampleRequest.builder()
                .name("Integration Test")
                .description("Created via integration test")
                .status(ExampleEnum.ACTIVE)
                .build();

        HttpHeaders headers = authHeaders();
        ResponseEntity<ExampleResponse> createResponse = restTemplate.exchange(
                url("/api/v1/examples"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ExampleResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Integration Test");

        ResponseEntity<ExampleResponse> getResponse = restTemplate.exchange(
                url("/api/v1/examples/" + createResponse.getBody().getId()),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ExampleResponse.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(createResponse.getBody().getId());
    }

    @Test
    void unauthenticatedRequest_shouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/v1/examples"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
