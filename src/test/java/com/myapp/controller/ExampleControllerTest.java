package com.myapp.controller;

import com.myapp.dto.response.ExampleResponse;
import com.myapp.model.enums.ExampleEnum;
import com.myapp.security.SecurityConfig;
import com.myapp.security.jwt.JwtAuthenticationFilter;
import com.myapp.security.jwt.JwtService;
import com.myapp.service.ExampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExampleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@ActiveProfiles("test")
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExampleService exampleService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void getAll_shouldReturnExamples() throws Exception {
        UUID id = UUID.randomUUID();
        ExampleResponse response = ExampleResponse.builder()
                .id(id)
                .name("Test")
                .description("Desc")
                .status(ExampleEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(exampleService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test"));
    }

    @Test
    void getAll_withoutAuth_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/examples"))
                .andExpect(status().isUnauthorized());
    }
}
