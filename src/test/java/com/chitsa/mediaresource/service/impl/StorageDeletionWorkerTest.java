package com.chitsa.mediaresource.service.impl;

import com.chitsa.mediaresource.entity.StorageDeletion;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.repository.StorageDeletionRepository;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageDeletionWorkerTest {

    @Mock
    private StorageDeletionRepository repository;

    @Mock
    private StorageDeletionService storageDeletionService;

    @Mock
    private ObjectStorageStrategy objectStorageStrategy;

    @InjectMocks
    private StorageDeletionWorker worker;

    @Test
    void retryDeletesTheObjectAndClearsTheRow() {
        when(repository.findByAttemptsLessThanOrderByCreateTimeAsc(any(Integer.class), any())).thenReturn(List.of(row()));

        worker.retry();

        verify(objectStorageStrategy).deleteObject("account-statements", "accounts/statement.pdf");
        verify(storageDeletionService).complete("account-statements", "accounts/statement.pdf");
    }

    @Test
    void retryRecordsAFailedDelete() {
        when(repository.findByAttemptsLessThanOrderByCreateTimeAsc(any(Integer.class), any())).thenReturn(List.of(row()));
        doThrow(new ObjectStorageApiException("down")).when(objectStorageStrategy)
                .deleteObject("account-statements", "accounts/statement.pdf");

        worker.retry();

        verify(storageDeletionService).recordAttempt("account-statements", "accounts/statement.pdf");
    }

    private static StorageDeletion row() {
        var row = new StorageDeletion();
        row.setContext("account-statements");
        row.setObjectKey("accounts/statement.pdf");
        row.setAttempts(0);
        row.setCreateTime(Instant.parse("2026-01-01T00:00:00Z"));
        return row;
    }
}
