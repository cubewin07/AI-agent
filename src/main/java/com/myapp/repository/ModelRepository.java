package com.myapp.repository;

import com.myapp.model.ModelEntity;
import com.myapp.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelRepository extends JpaRepository<ModelEntity, UUID> {
    List<ModelEntity> findByOwner(UserEntity owner);
    Optional<ModelEntity> findByIdAndOwner(UUID id, UserEntity owner);
}
