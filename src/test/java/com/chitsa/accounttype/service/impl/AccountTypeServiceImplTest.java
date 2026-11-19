package com.chitsa.accounttype.service.impl;

import com.chitsa.accounttype.entity.AccountType;
import com.chitsa.accounttype.exception.AccountTypeNotFoundException;
import com.chitsa.accounttype.mapper.AccountTypeMapper;
import com.chitsa.accounttype.repository.AccountTypeRepository;
import com.chitsa.accounttype.util.AccountTypeConstant;
import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.exception.InvalidParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTypeServiceImplTest {

    @Mock
    private AccountTypeRepository repository;

    private AccountTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountTypeServiceImpl(repository, new AccountTypeMapper(), new AccountTypeCache());
    }

    @Test
    void findAllReturnsTheRepositoryOrder() {
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(type("BUSINESS", "Business"), type("SAVINGS", "Savings")));

        var types = service.findAll();

        assertThat(types).extracting(type -> type.code()).containsExactly("BUSINESS", "SAVINGS");
        assertThat(types).extracting(type -> type.label()).containsExactly("Business", "Savings");
    }

    @Test
    void findByIdReturnsTheMatch() {
        var id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        var entity = type("CREDIT_CARD", "Credit card");
        entity.setId(id);
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(entity));

        var response = service.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.code()).isEqualTo("CREDIT_CARD");
        assertThat(response.label()).isEqualTo("Credit card");
    }

    @Test
    void findByIdThrowsWhenTheTypeIsMissing() {
        var id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AccountTypeNotFoundException.class)
                .hasMessage(AccountTypeConstant.NOT_FOUND + id);
    }

    @Test
    void requireExistsAcceptsAStoredId() {
        var id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        var entity = type("SAVINGS", "Savings");
        entity.setId(id);
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(entity));

        service.requireExists(id);
    }

    @Test
    void requireExistsThrowsWhenTheTypeIsMissing() {
        var id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> service.requireExists(id))
                .isInstanceOf(AccountTypeNotFoundException.class)
                .hasMessage(AccountTypeConstant.NOT_FOUND + id);
    }

    @Test
    void createDropsTheCacheSoTheNextReadSeesTheType() {
        var existing = type("SAVINGS", "Savings");
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(existing));
        service.findAll();

        when(repository.existsByCode("LOAN")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            AccountType entity = invocation.getArgument(0);
            entity.setId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
            return entity;
        });
        var created = type("LOAN", "Loan");
        created.setId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(existing, created));

        service.create("loan", " Loan ");

        assertThat(service.findAll()).extracting(type -> type.code()).containsExactly("SAVINGS", "LOAN");
    }

    @Test
    void createRejectsADuplicateCode() {
        when(repository.existsByCode("SAVINGS")).thenReturn(true);

        assertThatThrownBy(() -> service.create("savings", "Savings"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(AccountTypeConstant.DUPLICATE + "SAVINGS");
    }

    @Test
    void createRejectsABlankCode() {
        assertThatThrownBy(() -> service.create(" ", "Savings"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(AccountTypeConstant.CODE_REQUIRED);
    }

    private static AccountType type(String code, String label) {
        var entity = new AccountType();
        entity.setId(UUID.randomUUID());
        entity.setCode(code);
        entity.setLabel(label);
        return entity;
    }
}
