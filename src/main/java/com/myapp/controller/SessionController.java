package com.myapp.controller;

import com.myapp.dto.request.ChatRequest;
import com.myapp.dto.request.CreateSessionRequest;
import com.myapp.dto.response.ChatMessageResponse;
import com.myapp.dto.response.SessionResponse;
import com.myapp.model.UserEntity;
import com.myapp.repository.UserRepository;
import com.myapp.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return sessionService.create(request, user);
    }

    @GetMapping
    public List<SessionResponse> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return sessionService.getAll(user);
    }

    @GetMapping("/{id}")
    public SessionResponse getById(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return sessionService.getById(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        sessionService.delete(id, user);
    }

    @PostMapping("/{id}/chat")
    public String chat(@PathVariable UUID id, @Valid @RequestBody ChatRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return sessionService.chat(id, request.getQuery(), user);
    }

    @GetMapping("/{id}/history")
    public List<ChatMessageResponse> getHistory(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return sessionService.getHistory(id, user);
    }

    private UserEntity getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
    }
}
