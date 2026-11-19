package com.chitsa.mediaresource.service.impl;

import com.chitsa.mediaresource.entity.StorageDeletion;
import com.chitsa.mediaresource.repository.StorageDeletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Records S3 deletes that still need to finish. The database row is the source of truth. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDeletionService {

    public static final int MAX_ATTEMPTS = 10;

    private final StorageDeletionRepository repository;

    @Transactional
    public void enqueue(String context, String objectKey) {
        saveIfMissing(context, objectKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueInNewTransaction(String context, String objectKey) {
        saveIfMissing(context, objectKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String context, String objectKey) {
        repository.deleteByContextAndObjectKey(context, objectKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(String context, String objectKey) {
        var row = repository.findByContextAndObjectKey(context, objectKey).orElse(null);
        if (row == null || row.getAttempts() >= MAX_ATTEMPTS) {
            return;
        }
        row.setAttempts(row.getAttempts() + 1);
        if (row.getAttempts() >= MAX_ATTEMPTS) {
            log.error("Storage deletion gave up context={} objectKey={} attempts={}",
                    context, objectKey, row.getAttempts());
        }
    }

    private void saveIfMissing(String context, String objectKey) {
        if (repository.findByContextAndObjectKey(context, objectKey).isPresent()) {
            return;
        }
        var row = new StorageDeletion();
        row.setContext(context);
        row.setObjectKey(objectKey);
        row.setAttempts(0);
        row.setCreateTime(Instant.now());
        repository.save(row);
    }
}
