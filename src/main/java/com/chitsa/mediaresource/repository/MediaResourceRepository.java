package com.chitsa.mediaresource.repository;

import com.chitsa.mediaresource.entity.MediaResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaResourceRepository extends JpaRepository<MediaResource, UUID> {

    Optional<MediaResource> findByObjectKey(String objectKey);

    boolean existsByObjectKey(String objectKey);
}
