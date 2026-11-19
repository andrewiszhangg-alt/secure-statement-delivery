package com.chitsa.accountstatement.service.impl;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.mapper.AccountMapper;
import com.chitsa.account.service.AccountAccessGuard;
import com.chitsa.account.service.impl.AccountServiceImpl;
import com.chitsa.account.validator.AccountValidator;
import com.chitsa.accountstatement.config.AccountStatementProperties;
import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.exception.AccountStatementNotFoundException;
import com.chitsa.accountstatement.mapper.AccountStatementMapper;
import com.chitsa.accountstatement.repository.AccountStatementRepository;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.accountstatement.validator.AccountStatementValidator;
import com.chitsa.accounttype.mapper.AccountTypeMapper;
import com.chitsa.accounttype.service.impl.AccountTypeCache;
import com.chitsa.accounttype.service.impl.AccountTypeServiceImpl;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.mediaresource.config.MediaProperties;
import com.chitsa.mediaresource.mapper.MediaResourceMapper;
import com.chitsa.mediaresource.repository.MediaResourceRepository;
import com.chitsa.mediaresource.service.impl.MediaResourceServiceImpl;
import com.chitsa.mediaresource.service.impl.StorageDeletionService;
import com.chitsa.mediaresource.signature.MediaSignatureRules;
import com.chitsa.mediaresource.strategy.ObjectStorageStrategy;
import com.chitsa.mediaresource.validator.MediaResourceValidator;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.mapper.UserProfileMapper;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.service.impl.UserProfileServiceImpl;
import com.chitsa.userprofile.validator.UserProfileValidator;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        AccountStatementServiceImpl.class,
        AccountStatementMapper.class,
        AccountStatementValidator.class,
        AccountServiceImpl.class,
        AccountMapper.class,
        AccountValidator.class,
        AccountAccessGuard.class,
        UserProfileServiceImpl.class,
        UserProfileMapper.class,
        UserProfileValidator.class,
        AccountTypeServiceImpl.class,
        AccountTypeCache.class,
        AccountTypeMapper.class,
        MediaResourceServiceImpl.class,
        MediaResourceMapper.class,
        MediaResourceValidator.class,
        MediaSignatureRules.class,
        StorageDeletionService.class,
        AccountStatementServiceIT.TestBeans.class,
        FlywayAutoConfiguration.class
})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "DB_USER=test",
        "DB_PASSWORD=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class AccountStatementServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountStatementServiceImpl statements;

    @Autowired
    private AccountServiceImpl accounts;

    @Autowired
    private UserProfileServiceImpl profiles;

    @Autowired
    private AccountTypeServiceImpl accountTypes;

    @Autowired
    private AccountStatementRepository statementRepository;

    @Autowired
    private MediaResourceRepository mediaRepository;

    @Autowired
    private AccountStatementProperties properties;

    @Autowired
    private SignedInCaller caller;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createStoresTheFileUnderTheAccountAndListsNewestFirst() {
        var accountId = openAccount();
        var older = statements.create(upload(accountId, "older.pdf"));
        var newer = statements.create(upload(accountId, "newer.pdf"));
        stamp(older.id(), Instant.parse("2026-01-01T00:00:00Z"));
        stamp(newer.id(), Instant.parse("2026-06-01T00:00:00Z"));
        properties.setPageSize(1);
        try {
            var page = statements.findByAccountId(accountId, -1);

            var stored = statementRepository.findById(newer.id()).orElseThrow();
            var media = mediaRepository.findById(stored.getMediaResourceId()).orElseThrow();
            assertThat(media.getObjectKey()).startsWith(accountId + "/").endsWith(".pdf");
            assertThat(page.getNumber()).isZero();
            assertThat(page.getContent()).extracting(item -> item.id()).containsExactly(newer.id());
        } finally {
            properties.setPageSize(5);
        }
    }

    @Test
    void aStatementFromAnotherAccountIsNotFound() {
        var accountId = openAccount();
        var statementId = statements.create(upload(accountId, "statement.pdf")).id();
        var otherAccountId = accounts.create(new AccountRequestDto(caller.profileId, typeId("CREDIT_CARD"))).id();

        assertThatThrownBy(() -> statements.generatePresignedUrl(otherAccountId, statementId))
                .isInstanceOf(AccountStatementNotFoundException.class)
                .hasMessage(AccountStatementConstant.NOT_FOUND.formatted(otherAccountId, statementId));
    }

    @Test
    void deleteRemovesTheLinkAndTheMediaRow() {
        var accountId = openAccount();
        var statementId = statements.create(upload(accountId, "statement.pdf")).id();
        var mediaId = statementRepository.findById(statementId).orElseThrow().getMediaResourceId();

        statements.deleteById(accountId, statementId);

        assertThat(statementRepository.findById(statementId)).isEmpty();
        assertThat(mediaRepository.findById(mediaId)).isEmpty();
    }

    private UUID openAccount() {
        var profile = profiles.create(new UserProfileRequestDto(
                UUID.randomUUID(), "ada@example.com", "Ada", "Lovelace"));
        caller.profileId = profile.id();
        return accounts.create(new AccountRequestDto(profile.id(), typeId("SAVINGS"))).id();
    }

    private UUID typeId(String code) {
        return accountTypes.findAll().stream()
                .filter(type -> code.equals(type.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private static AccountStatementRequestDto upload(UUID accountId, String filename) {
        MultipartFile file = new MockMultipartFile("file", filename, "application/pdf", "%PDF-1.4".getBytes());
        return new AccountStatementRequestDto(accountId, file);
    }

    private void stamp(UUID statementId, Instant created) {
        entityManager.flush();
        entityManager.createNativeQuery("update account_statements set create_time = :created where id = :id")
                .setParameter("created", created)
                .setParameter("id", statementId)
                .executeUpdate();
        entityManager.clear();
    }

    static class SignedInCaller implements CurrentUser, CurrentProfileProvider {

        private UUID profileId;

        @Override
        public UUID profileId() {
            return profileId;
        }

        @Override
        public boolean profileComplete() {
            return true;
        }

        @Override
        public UUID idpUuid() {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        @Override
        public String email() {
            return "ada@example.com";
        }

        @Override
        public String firstName() {
            return "Ada";
        }

        @Override
        public String lastName() {
            return "Lovelace";
        }

        @Override
        public boolean isAdmin() {
            return false;
        }
    }

    static class RecordingStorage implements ObjectStorageStrategy {

        @Override
        public String uploadObject(String bucket, String objectKey, MultipartFile multipartFile) {
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
        AccountStatementProperties accountStatementProperties() {
            return new AccountStatementProperties();
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

        @Bean
        SignedInCaller signedInCaller() {
            return new SignedInCaller();
        }
    }
}
