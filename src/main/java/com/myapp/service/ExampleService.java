package com.myapp.service;

import com.myapp.dto.request.CreateExampleRequest;
import com.myapp.dto.response.ExampleResponse;

import java.util.List;
import java.util.UUID;

public interface ExampleService {

    ExampleResponse create(CreateExampleRequest request);

    ExampleResponse getById(UUID id);

    List<ExampleResponse> getAll();
}
