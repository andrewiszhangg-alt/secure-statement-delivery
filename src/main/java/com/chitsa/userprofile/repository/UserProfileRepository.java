package com.chitsa.userprofile.repository;

import com.chitsa.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByIdpUuid(UUID idpUuid);

    boolean existsByIdpUuid(UUID idpUuid);
}
