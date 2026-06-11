package com.myapp.repository;

import com.myapp.model.SessionEntity;
import com.myapp.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    List<SessionEntity> findByOwner(UserEntity owner);
    Optional<SessionEntity> findByIdAndOwner(UUID id, UserEntity owner);
}
