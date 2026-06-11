package com.myapp.controller;

import com.myapp.dto.request.CreateModelRequest;
import com.myapp.dto.response.ModelResponse;
import com.myapp.model.UserEntity;
import com.myapp.repository.UserRepository;
import com.myapp.service.ModelService;
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
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModelResponse create(@Valid @RequestBody CreateModelRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return modelService.create(request, user);
    }

    @GetMapping
    public List<ModelResponse> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return modelService.getAll(user);
    }

    @GetMapping("/{id}")
    public ModelResponse getById(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        return modelService.getById(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = getCurrentUser(userDetails);
        modelService.delete(id, user);
    }

    private UserEntity getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
    }
}
