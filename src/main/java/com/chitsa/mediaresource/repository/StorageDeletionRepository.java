package com.chitsa.mediaresource.repository;

import com.chitsa.mediaresource.entity.StorageDeletion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageDeletionRepository extends JpaRepository<StorageDeletion, UUID> {

    Optional<StorageDeletion> findByContextAndObjectKey(String context, String objectKey);

    List<StorageDeletion> findByAttemptsLessThanOrderByCreateTimeAsc(int attempts, Pageable pageable);

    void deleteByContextAndObjectKey(String context, String objectKey);
}
