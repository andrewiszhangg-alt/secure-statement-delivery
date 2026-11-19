package com.chitsa.accounttype.service.impl;

import com.chitsa.accounttype.exception.AccountTypeNotFoundException;
import com.chitsa.accounttype.mapper.AccountTypeMapper;
import com.chitsa.accounttype.util.AccountTypeConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountTypeServiceImpl.class, AccountTypeMapper.class, AccountTypeCache.class, FlywayAutoConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "DB_USER=test",
        "DB_PASSWORD=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class AccountTypeServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountTypeServiceImpl service;

    @Test
    void findAllReturnsTheSeededTypesByLabel() {
        var types = service.findAll();

        assertThat(types).extracting(type -> type.code())
                .containsExactly("BUSINESS", "CREDIT_CARD", "PERSONAL", "SAVINGS");
        assertThat(types).extracting(type -> type.label())
                .containsExactly("Business", "Credit card", "Personal", "Savings");
    }

    @Test
    void findByIdReturnsASeededType() {
        var savings = service.findAll().stream()
                .filter(type -> "SAVINGS".equals(type.code()))
                .findFirst()
                .orElseThrow();

        var found = service.findById(savings.id());

        assertThat(found.code()).isEqualTo("SAVINGS");
        assertThat(found.label()).isEqualTo("Savings");
        service.requireExists(savings.id());
    }

    @Test
    void findByIdThrowsWhenTheTypeIsMissing() {
        var id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AccountTypeNotFoundException.class)
                .hasMessage(AccountTypeConstant.NOT_FOUND + id);
    }
}
