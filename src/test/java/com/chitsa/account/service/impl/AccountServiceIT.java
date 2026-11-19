package com.chitsa.account.service.impl;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.mapper.AccountMapper;
import com.chitsa.account.service.AccountAccessGuard;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.account.validator.AccountValidator;
import com.chitsa.accounttype.mapper.AccountTypeMapper;
import com.chitsa.accounttype.service.impl.AccountTypeCache;
import com.chitsa.accounttype.service.impl.AccountTypeServiceImpl;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.mapper.UserProfileMapper;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.service.impl.UserProfileServiceImpl;
import com.chitsa.userprofile.validator.UserProfileValidator;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
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
        AccountServiceIT.TestBeans.class,
        FlywayAutoConfiguration.class
})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "DB_USER=test",
        "DB_PASSWORD=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class AccountServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountServiceImpl accounts;

    @Autowired
    private UserProfileServiceImpl profiles;

    @Autowired
    private AccountTypeServiceImpl accountTypes;

    @Autowired
    private SignedInCaller caller;

    @Test
    void createStoresTheOwnerAndUpdateChangesOnlyTheType() {
        var profileId = signIn();
        var savings = typeId("SAVINGS");
        var credit = typeId("CREDIT_CARD");

        var created = accounts.create(new AccountRequestDto(profileId, savings));
        var updated = accounts.update(created.id(), new AccountRequestDto(UUID.randomUUID(), credit));
        var listed = accounts.findByUserProfileId(profileId);

        assertThat(created.accountType().code()).isEqualTo("SAVINGS");
        assertThat(updated.userProfileId()).isEqualTo(profileId);
        assertThat(updated.accountType().code()).isEqualTo("CREDIT_CARD");
        assertThat(listed).extracting(account -> account.accountType().code()).containsExactly("CREDIT_CARD");
    }

    @Test
    void foreignAccountIsNotFoundAndAForeignProfileIsForbidden() {
        var ownerId = signIn();
        var accountId = accounts.create(new AccountRequestDto(ownerId, typeId("PERSONAL"))).id();
        var strangerId = signIn();

        assertThatThrownBy(() -> accounts.findById(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(AccountConstant.NOT_FOUND + accountId);
        assertThatThrownBy(() -> accounts.findByUserProfileId(ownerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccountConstant.PROFILE_ACCESS_DENIED);
        assertThat(strangerId).isNotEqualTo(ownerId);
    }

    @Test
    void deleteRemovesTheAccount() {
        var profileId = signIn();
        var accountId = accounts.create(new AccountRequestDto(profileId, typeId("BUSINESS"))).id();

        accounts.deleteById(accountId);

        assertThatThrownBy(() -> accounts.findById(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(AccountConstant.NOT_FOUND + accountId);
    }

    private UUID signIn() {
        var profile = profiles.create(new UserProfileRequestDto(
                UUID.randomUUID(), "ada@example.com", "Ada", "Lovelace"));
        caller.profileId = profile.id();
        return profile.id();
    }

    private UUID typeId(String code) {
        return accountTypes.findAll().stream()
                .filter(type -> code.equals(type.code()))
                .findFirst()
                .orElseThrow()
                .id();
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
        SignedInCaller signedInCaller() {
            return new SignedInCaller();
        }
    }
}
