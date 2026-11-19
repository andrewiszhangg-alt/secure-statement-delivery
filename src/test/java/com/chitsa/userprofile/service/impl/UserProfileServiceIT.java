package com.chitsa.userprofile.service.impl;

import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.mapper.UserProfileMapper;
import com.chitsa.userprofile.repository.UserProfileRepository;
import com.chitsa.userprofile.util.UserProfileConstant;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
        UserProfileServiceImpl.class,
        UserProfileMapper.class,
        UserProfileValidator.class,
        UserProfileServiceIT.ValidationBeans.class,
        FlywayAutoConfiguration.class
})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "DB_USER=test",
        "DB_PASSWORD=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class UserProfileServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private UserProfileServiceImpl service;

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createRejectsADuplicateIdpUuid() {
        var idpUuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        service.create(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        assertThatThrownBy(() -> service.create(request(idpUuid, "ada@example.com", "Ada", "Lovelace")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(UserProfileConstant.DUPLICATE + idpUuid);
    }

    @Test
    void updateKeepsTheIdpUuidAndEmail() {
        var idpUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        var created = service.create(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        var updated = service.update(created.id(),
                new UserProfileRequestDto(UUID.randomUUID(), "other@example.com", "Grace", "Hopper"));
        var stored = service.findByIdpUuid(idpUuid);

        assertThat(updated.firstName()).isEqualTo("Grace");
        assertThat(updated.lastName()).isEqualTo("Hopper");
        assertThat(stored.email()).isEqualTo("ada@example.com");
        assertThat(stored.id()).isEqualTo(created.id());
    }

    @Test
    void createStartsAtVersionZeroAndAStaleCopyConflicts() {
        var created = service.create(request(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "ada@example.com", "Ada", "Lovelace"));
        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.getVersion()).isZero();

        entityManager.flush();
        entityManager.detach(stored);
        stored.setFirstName("Grace");

        var current = repository.findById(created.id()).orElseThrow();
        current.setLastName("Hopper");
        repository.saveAndFlush(current);

        assertThat(current.getVersion()).isEqualTo(1L);
        assertThatThrownBy(() -> repository.saveAndFlush(stored))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private static UserProfileRequestDto request(UUID idpUuid, String email, String firstName, String lastName) {
        return new UserProfileRequestDto(idpUuid, email, firstName, lastName);
    }

    static class ValidationBeans {

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        RequestBeanValidator requestBeanValidator(Validator validator) {
            return new RequestBeanValidator(validator);
        }
    }
}
