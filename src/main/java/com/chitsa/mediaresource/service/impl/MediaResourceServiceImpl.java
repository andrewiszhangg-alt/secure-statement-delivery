package com.chitsa.mediaresource.service.impl;

import com.chitsa.common.util.ValidationUtil;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.dto.MediaResourceResponseDto;
import com.chitsa.mediaresource.entity.MediaResource;
import com.chitsa.mediaresource.exception.MediaResourceNotFoundException;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.mapper.MediaResourceMapper;
import com.chitsa.mediaresource.repository.MediaResourceRepository;
import com.chitsa.mediaresource.service.MediaResourceService;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.mediaresource.validator.MediaResourceValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaResourceServiceImpl implements MediaResourceService {

    private final MediaResourceRepository repository;
    private final MediaResourceMapper mapper;
    private final MediaResourceValidator validator;
    private final ObjectStorageStrategy objectStorageStrategy;
    private final StorageDeletionService storageDeletions;

    @Transactional
    @Override
    public MediaResourceResponseDto create(MediaResourceRequestDto requestDto) {
        validator.validate(requestDto);
        ValidationUtil.require(
                !repository.existsByObjectKey(requestDto.objectKey()),
                MediaResourceConstant.DUPLICATE_OBJECT_KEY + requestDto.objectKey());

        var entity = mapper.convertDtoToEntity(requestDto);
        var url = objectStorageStrategy.uploadObject(
                requestDto.context(),
                requestDto.objectKey(),
                requestDto.multipartFile());
        entity.setUrl(url);

        var saved = saveOrRemoveUpload(entity);
        log.info("Created MediaResource id={} objectKey={}", saved.getId(), saved.getObjectKey());
        return mapper.convertEntityToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResourceResponseDto findById(UUID id) {
        return mapper.convertEntityToResponseDto(findEntityById(id));
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        deleteWithStorage(findEntityById(id));
    }

    @Transactional
    @Override
    public void deleteByObjectKey(String objectKey) {
        deleteWithStorage(findByObjectKey(objectKey));
    }

    @Transactional(readOnly = true)
    @Override
    public String generatePreSignedUrlForResource(UUID id) {
        var mediaResource = findEntityById(id);
        return objectStorageStrategy.generatePreSignedUrlForObject(
                mediaResource.getId(), mediaResource.getContext(), mediaResource.getObjectKey());
    }

    @Override
    public String generatePreSignedUrlForResource(String context, String objectKey) {
        return objectStorageStrategy.generatePreSignedUrlForObject(context, objectKey);
    }

    /** Deletes the row, then the object after commit. With no transaction, the object is removed immediately. */
    private void deleteWithStorage(MediaResource mediaResource) {
        repository.delete(mediaResource);
        storageDeletions.enqueue(mediaResource.getContext(), mediaResource.getObjectKey());
        removeObjectAfterCommit(mediaResource.getId(), mediaResource.getContext(), mediaResource.getObjectKey());
        log.info("Deleted MediaResource id={} objectKey={}", mediaResource.getId(), mediaResource.getObjectKey());
    }

    /** Removes the uploaded object when the save rolls back, and when a save outside a transaction fails. */
    private MediaResource saveOrRemoveUpload(MediaResource entity) {
        var transactional = TransactionSynchronizationManager.isSynchronizationActive();
        if (transactional) {
            removeObjectOnRollback(entity.getContext(), entity.getObjectKey());
        }
        try {
            return repository.save(entity);
        } catch (RuntimeException ex) {
            if (!transactional) {
                log.warn("Save failed; removing uploaded objectKey={}", entity.getObjectKey());
                removeOrEnqueue(entity.getContext(), entity.getObjectKey());
            }
            throw ex;
        }
    }

    /** Undoes an upload when the surrounding transaction rolls back. */
    private void removeObjectOnRollback(String context, String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("Rolled back; removing uploaded objectKey={}", objectKey);
                    removeOrEnqueue(context, objectKey);
                }
            }
        });
    }

    private void removeObjectAfterCommit(UUID mediaId, String context, String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            finishDelete(mediaId, context, objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                finishDelete(mediaId, context, objectKey);
            }
        });
    }

    /** The cleanup row was written with the delete. Success removes it; failure leaves it to be retried. */
    private void finishDelete(UUID mediaId, String context, String objectKey) {
        try {
            deleteStoredObject(mediaId, context, objectKey);
            storageDeletions.complete(context, objectKey);
        } catch (ObjectStorageApiException e) {
            orphaned(context, objectKey, e);
            storageDeletions.recordAttempt(context, objectKey);
        }
    }

    /** Upload cleanup has no prior row. A failed delete is recorded in a new transaction. */
    private void removeOrEnqueue(String context, String objectKey) {
        try {
            deleteStoredObject(null, context, objectKey);
        } catch (ObjectStorageApiException e) {
            orphaned(context, objectKey, e);
            storageDeletions.enqueueInNewTransaction(context, objectKey);
        }
    }

    private void deleteStoredObject(UUID mediaId, String context, String objectKey) {
        if (mediaId == null) {
            objectStorageStrategy.deleteObject(context, objectKey);
        } else {
            objectStorageStrategy.deleteObject(mediaId, context, objectKey);
        }
    }

    private static void orphaned(String context, String objectKey, ObjectStorageApiException e) {
        log.error("Orphaned S3 object context={} objectKey={}: {}", context, objectKey, e.getMessage());
    }

    private MediaResource findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new MediaResourceNotFoundException(MediaResourceConstant.NOT_FOUND + id));
    }

    private MediaResource findByObjectKey(String objectKey) {
        return repository.findByObjectKey(objectKey)
                .orElseThrow(() -> new MediaResourceNotFoundException(MediaResourceConstant.NOT_FOUND_FOR_KEY + objectKey));
    }
}
