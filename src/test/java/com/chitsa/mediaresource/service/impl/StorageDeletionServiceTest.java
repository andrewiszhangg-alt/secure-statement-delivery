package com.chitsa.mediaresource.service.impl;

import com.chitsa.mediaresource.entity.StorageDeletion;
import com.chitsa.mediaresource.repository.StorageDeletionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageDeletionServiceTest {

    @Mock
    private StorageDeletionRepository repository;

    private StorageDeletionService service;

    @BeforeEach
    void setUp() {
        service = new StorageDeletionService(repository);
    }

    @Test
    void enqueueStoresANewRow() {
        when(repository.findByContextAndObjectKey("bucket", "key")).thenReturn(Optional.empty());

        service.enqueue("bucket", "key");

        var saved = ArgumentCaptor.forClass(StorageDeletion.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAttempts()).isZero();
        assertThat(saved.getValue().getObjectKey()).isEqualTo("key");
    }

    @Test
    void enqueueSkipsARowThatAlreadyExists() {
        when(repository.findByContextAndObjectKey("bucket", "key")).thenReturn(Optional.of(new StorageDeletion()));

        service.enqueueInNewTransaction("bucket", "key");

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordAttemptStopsAtTheCap() {
        var row = new StorageDeletion();
        row.setAttempts(StorageDeletionService.MAX_ATTEMPTS - 1);
        when(repository.findByContextAndObjectKey("bucket", "key")).thenReturn(Optional.of(row));

        service.recordAttempt("bucket", "key");

        assertThat(row.getAttempts()).isEqualTo(StorageDeletionService.MAX_ATTEMPTS);
    }

    @Test
    void completeRemovesTheRow() {
        service.complete("bucket", "key");

        verify(repository).deleteByContextAndObjectKey("bucket", "key");
    }
}
