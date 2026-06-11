package com.myapp.repository;

import com.myapp.model.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExampleRepository extends JpaRepository<ExampleEntity, UUID> {
}
