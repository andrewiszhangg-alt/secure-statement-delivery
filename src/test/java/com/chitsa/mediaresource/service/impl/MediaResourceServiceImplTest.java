package com.chitsa.mediaresource.service.impl;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.mediaresource.config.MediaProperties;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.entity.MediaResource;
import com.chitsa.mediaresource.exception.MediaResourceNotFoundException;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.mapper.MediaResourceMapper;
import com.chitsa.mediaresource.repository.MediaResourceRepository;
import com.chitsa.mediaresource.signature.MediaSignatureRules;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.mediaresource.validator.MediaResourceValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaResourceServiceImplTest {

    @Mock
    private MediaResourceRepository repository;

    @Mock
    private StorageDeletionService storageDeletions;

    private final FakeStorage storage = new FakeStorage();

    private MediaResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        var properties = new MediaProperties();
        properties.setAllowedContentTypes(List.of("application/pdf"));
        properties.setAllowedExtensions(List.of("pdf"));
        var validator = new MediaResourceValidator(properties,
                new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()),
                new MediaSignatureRules());
        service = new MediaResourceServiceImpl(
                repository, new MediaResourceMapper(), validator, storage, storageDeletions);
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createStoresTheUploadedUrl() {
        when(repository.save(any())).thenAnswer(invocation -> {
            MediaResource entity = invocation.getArgument(0);
            entity.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
            return entity;
        });

        var response = service.create(request());

        assertThat(response.id()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(response.url()).isEqualTo("https://files.example/accounts/statement.pdf");
        assertThat(response.objectKey()).isEqualTo("accounts/statement.pdf");
        assertThat(storage.deleted).isEmpty();
    }

    @Test
    void createRejectsADuplicateKeyBeforeUploading() {
        when(repository.existsByObjectKey("accounts/statement.pdf")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.DUPLICATE_OBJECT_KEY + "accounts/statement.pdf");
        assertThat(storage.uploads).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void createRemovesTheUploadWhenTheSaveFailsOutsideATransaction() {
        when(repository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
        assertThat(storage.deleted).containsExactly("account-statements:accounts/statement.pdf");
    }

    @Test
    void rollbackRemovesTheUploadAndCommitLeavesIt() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();

        service.create(request());
        fire(TransactionSynchronization.STATUS_COMMITTED);
        assertThat(storage.deleted).isEmpty();

        fire(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(storage.deleted).containsExactly("account-statements:accounts/statement.pdf");
    }

    @Test
    void findByIdReturnsTheStoredResource() {
        var id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(repository.findById(id)).thenReturn(Optional.of(stored(id)));

        var response = service.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.objectKey()).isEqualTo("accounts/statement.pdf");
    }

    @Test
    void findByIdThrowsWhenTheResourceIsMissing() {
        var id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(MediaResourceNotFoundException.class)
                .hasMessage(MediaResourceConstant.NOT_FOUND + id);
    }

    @Test
    void deleteByIdRemovesTheObjectImmediatelyOutsideATransaction() {
        var id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(repository.findById(id)).thenReturn(Optional.of(stored(id)));

        service.deleteById(id);

        verify(repository).delete(any(MediaResource.class));
        assertThat(storage.deleted).containsExactly("account-statements:accounts/statement.pdf");
    }

    @Test
    void deleteWaitsForCommitInsideATransaction() {
        var id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(repository.findById(id)).thenReturn(Optional.of(stored(id)));
        TransactionSynchronizationManager.initSynchronization();

        service.deleteById(id);
        assertThat(storage.deleted).isEmpty();

        fireAfterCommit();
        assertThat(storage.deleted).containsExactly("account-statements:accounts/statement.pdf");
    }

    @Test
    void deleteByObjectKeyRemovesTheObject() {
        when(repository.findByObjectKey("accounts/statement.pdf")).thenReturn(Optional.of(stored(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))));

        service.deleteByObjectKey("accounts/statement.pdf");

        assertThat(storage.deleted).containsExactly("account-statements:accounts/statement.pdf");
    }

    @Test
    void deleteByObjectKeyThrowsWhenTheKeyIsMissing() {
        when(repository.findByObjectKey("missing.pdf")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteByObjectKey("missing.pdf"))
                .isInstanceOf(MediaResourceNotFoundException.class)
                .hasMessage(MediaResourceConstant.NOT_FOUND_FOR_KEY + "missing.pdf");
    }

    @Test
    void aFailedObjectDeleteLeavesTheOutcomeIntact() {
        var id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(repository.findById(id)).thenReturn(Optional.of(stored(id)));
        storage.deleteFailure = new ObjectStorageApiException("storage down");

        service.deleteById(id);

        verify(repository).delete(any(MediaResource.class));
    }

    @Test
    void presignUsesTheStoredContextAndKey() {
        var id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        when(repository.findById(id)).thenReturn(Optional.of(stored(id)));

        assertThat(service.generatePreSignedUrlForResource(id))
                .isEqualTo("https://signed.example/accounts/statement.pdf");
        assertThat(service.generatePreSignedUrlForResource("account-statements", "accounts/other.pdf"))
                .isEqualTo("https://signed.example/accounts/other.pdf");
    }

    private static void fire(int status) {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private static void fireAfterCommit() {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    private static MediaResourceRequestDto request() {
        MultipartFile file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "%PDF-1.4".getBytes());
        return new MediaResourceRequestDto("account-statements", "accounts/statement.pdf", file);
    }

    private static MediaResource stored(UUID id) {
        var entity = new MediaResource();
        entity.setId(id);
        entity.setContext("account-statements");
        entity.setObjectKey("accounts/statement.pdf");
        entity.setUrl("https://files.example/accounts/statement.pdf");
        return entity;
    }

    private static final class FakeStorage implements ObjectStorageStrategy {

        private final List<String> deleted = new ArrayList<>();
        private int uploads;
        private ObjectStorageApiException deleteFailure;

        @Override
        public String uploadObject(String bucket, String objectKey, MultipartFile multipartFile) {
            uploads++;
            return "https://files.example/" + objectKey;
        }

        @Override
        public void deleteObject(String bucket, String objectKey) {
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            deleted.add(bucket + ":" + objectKey);
        }

        @Override
        public String generatePreSignedUrlForObject(String bucket, String objectKey) {
            return "https://signed.example/" + objectKey;
        }
    }
}
