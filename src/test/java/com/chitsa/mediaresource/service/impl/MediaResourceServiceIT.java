package com.chitsa.mediaresource.service.impl;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.mediaresource.config.MediaProperties;
import com.chitsa.mediaresource.exception.MediaResourceNotFoundException;
import com.chitsa.mediaresource.mapper.MediaResourceMapper;
import com.chitsa.mediaresource.signature.MediaSignatureRules;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import com.chitsa.mediaresource.util.MediaResourceUtil;
import com.chitsa.mediaresource.validator.MediaResourceValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        MediaResourceServiceImpl.class,
        MediaResourceMapper.class,
        MediaResourceValidator.class,
        MediaSignatureRules.class,
        StorageDeletionService.class,
        MediaResourceServiceIT.TestBeans.class,
        FlywayAutoConfiguration.class
})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "DB_USER=test",
        "DB_PASSWORD=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class MediaResourceServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MediaResourceServiceImpl service;

    @Autowired
    private RecordingStorage storage;

    @BeforeEach
    void resetUploads() {
        storage.reset();
    }

    @Test
    void createPersistsTheUploadedObject() {
        var created = service.create(MediaResourceUtil.toCreateRequest(
                "account-statements", "accounts/statement.pdf", pdf("statement.pdf")));

        var stored = service.findById(created.id());

        assertThat(stored.objectKey()).isEqualTo("accounts/statement.pdf");
        assertThat(stored.url()).isEqualTo("https://files.example/accounts/statement.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.originalFilename()).isEqualTo("statement.pdf");
        assertThat(storage.uploads).isEqualTo(1);
    }

    @Test
    void createRejectsADuplicateObjectKey() {
        service.create(MediaResourceUtil.toCreateRequest(
                "account-statements", "accounts/statement.pdf", pdf("statement.pdf")));

        assertThatThrownBy(() -> service.create(MediaResourceUtil.toCreateRequest(
                "account-statements", "accounts/statement.pdf", pdf("statement.pdf"))))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(MediaResourceConstant.DUPLICATE_OBJECT_KEY + "accounts/statement.pdf");
        assertThat(storage.uploads).isEqualTo(1);
    }

    @Test
    void deleteRemovesTheRow() {
        var created = service.create(MediaResourceUtil.toCreateRequest(
                "account-statements", "accounts/other.pdf", pdf("other.pdf")));

        service.deleteById(created.id());

        assertThatThrownBy(() -> service.findById(created.id()))
                .isInstanceOf(MediaResourceNotFoundException.class)
                .hasMessage(MediaResourceConstant.NOT_FOUND + created.id());
    }

    private static MultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", "%PDF-1.4".getBytes());
    }

    static class RecordingStorage implements ObjectStorageStrategy {

        private int uploads;

        void reset() {
            uploads = 0;
        }

        @Override
        public String uploadObject(String bucket, String objectKey, MultipartFile multipartFile) {
            uploads++;
            return "https://files.example/" + objectKey;
        }

        @Override
        public void deleteObject(String bucket, String objectKey) {
        }

        @Override
        public String generatePreSignedUrlForObject(String bucket, String objectKey) {
            return "https://signed.example/" + objectKey;
        }
    }

    static class TestBeans {

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        RequestBeanValidator requestBeanValidator(Validator validator) {
            return new RequestBeanValidator(validator);
        }

        @Bean
        MediaProperties mediaProperties() {
            var properties = new MediaProperties();
            properties.setAllowedContentTypes(List.of("application/pdf"));
            properties.setAllowedExtensions(List.of("pdf"));
            return properties;
        }

        @Bean
        RecordingStorage recordingStorage() {
            return new RecordingStorage();
        }
    }
}
