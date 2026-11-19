package com.chitsa.mediaresource.service.impl;

import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.repository.StorageDeletionRepository;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries S3 deletes that were saved on {@code storage_deletions}. A missing object deletes cleanly. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDeletionWorker {

    private static final int BATCH = 20;

    private final StorageDeletionRepository repository;
    private final StorageDeletionService storageDeletionService;
    private final ObjectStorageStrategy objectStorageStrategy;

    @Scheduled(fixedDelayString = "${app.storage.cleanup-delay-ms:60000}")
    public void retry() {
        var due = repository.findByAttemptsLessThanOrderByCreateTimeAsc(
                StorageDeletionService.MAX_ATTEMPTS, PageRequest.of(0, BATCH));
        for (var row : due) {
            try {
                objectStorageStrategy.deleteObject(row.getContext(), row.getObjectKey());
                storageDeletionService.complete(row.getContext(), row.getObjectKey());
            } catch (ObjectStorageApiException ex) {
                log.warn("Storage deletion retry failed context={} objectKey={}: {}",
                        row.getContext(), row.getObjectKey(), ex.getMessage());
                storageDeletionService.recordAttempt(row.getContext(), row.getObjectKey());
            }
        }
    }
}
