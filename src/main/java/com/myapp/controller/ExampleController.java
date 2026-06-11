package com.myapp.controller;

import com.myapp.dto.request.CreateExampleRequest;
import com.myapp.dto.response.ExampleResponse;
import com.myapp.service.ExampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping
    public List<ExampleResponse> getAll() {
        return exampleService.getAll();
    }

    @GetMapping("/{id}")
    public ExampleResponse getById(@PathVariable UUID id) {
        return exampleService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExampleResponse create(@Valid @RequestBody CreateExampleRequest request) {
        return exampleService.create(request);
    }
}
