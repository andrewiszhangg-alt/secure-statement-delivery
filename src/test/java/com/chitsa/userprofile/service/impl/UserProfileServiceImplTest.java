package com.chitsa.userprofile.service.impl;

import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.entity.UserProfile;
import com.chitsa.userprofile.exception.UserProfileNotFoundException;
import com.chitsa.userprofile.mapper.UserProfileMapper;
import com.chitsa.userprofile.repository.UserProfileRepository;
import com.chitsa.userprofile.util.UserProfileConstant;
import com.chitsa.userprofile.validator.UserProfileValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository repository;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        var validator = new UserProfileValidator(
                new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        service = new UserProfileServiceImpl(repository, new UserProfileMapper(), validator, directTransactions());
    }

    @Test
    void createRejectsADuplicateIdpUuid() {
        var idpUuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(repository.existsByIdpUuid(idpUuid)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(idpUuid, "ada@example.com", "Ada", "Lovelace")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(UserProfileConstant.DUPLICATE + idpUuid);
        verify(repository, never()).save(any());
    }

    @Test
    void createSetsTheIdpUuidOnce() {
        var idpUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(repository.existsByIdpUuid(idpUuid)).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> {
            UserProfile entity = invocation.getArgument(0);
            entity.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
            return entity;
        });

        var response = service.create(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        var saved = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getIdpUuid()).isEqualTo(idpUuid);
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(response.id()).isEqualTo(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void updateChangesNamesAndLeavesIdentity() {
        var id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        var idpUuid = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        var entity = stored(id, idpUuid, "ada@example.com", "Ada", "Lovelace");
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        var response = service.update(id, new UserProfileRequestDto(UUID.randomUUID(), "other@example.com", "Grace", "Hopper"));

        assertThat(entity.getIdpUuid()).isEqualTo(idpUuid);
        assertThat(entity.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.firstName()).isEqualTo("Grace");
        assertThat(response.lastName()).isEqualTo("Hopper");
    }

    @Test
    void findOrCreateReturnsTheExistingProfile() {
        var idpUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        var entity = stored(UUID.randomUUID(), idpUuid, "ada@example.com", "Ada", "Lovelace");
        when(repository.findByIdpUuid(idpUuid)).thenReturn(Optional.of(entity));

        var response = service.findOrCreateByIdpUuid(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        assertThat(response.email()).isEqualTo("ada@example.com");
        verify(repository, never()).save(any());
    }

    @Test
    void findOrCreateCreatesAMissingProfile() {
        var idpUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(repository.findByIdpUuid(idpUuid)).thenReturn(Optional.empty());
        when(repository.existsByIdpUuid(idpUuid)).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.findOrCreateByIdpUuid(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        verify(repository).saveAndFlush(any());
    }

    @Test
    void findOrCreateReadsTheRowThatWonTheUniqueKey() {
        var idpUuid = UUID.fromString("12121212-1212-1212-1212-121212121212");
        var entity = stored(UUID.randomUUID(), idpUuid, "ada@example.com", "Ada", "Lovelace");
        when(repository.findByIdpUuid(idpUuid)).thenReturn(Optional.empty(), Optional.of(entity));
        when(repository.existsByIdpUuid(idpUuid)).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        var response = service.findOrCreateByIdpUuid(request(idpUuid, "ada@example.com", "Ada", "Lovelace"));

        assertThat(response.id()).isEqualTo(entity.getId());
        verify(repository, never()).save(any());
    }

    @Test
    void requireCompleteProfileRejectsAnIncompleteProfile() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(stored(id, UUID.randomUUID(), "ada@example.com", null, null)));

        assertThatThrownBy(() -> service.requireCompleteProfile(id))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(UserProfileConstant.PROFILE_INCOMPLETE);
    }

    @Test
    void requireCompleteProfileAcceptsACompleteProfile() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(
                stored(id, UUID.randomUUID(), "ada@example.com", "Ada", "Lovelace")));

        service.requireCompleteProfile(id);
        assertThat(service.isProfileComplete(id)).isTrue();
    }

    @Test
    void findByIdThrowsWhenTheProfileIsMissing() {
        var id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage(UserProfileConstant.NOT_FOUND + id);
    }

    @Test
    void findByIdpUuidThrowsWhenTheProfileIsMissing() {
        var idpUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(repository.findByIdpUuid(idpUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdpUuid(idpUuid))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage(UserProfileConstant.NOT_FOUND_FOR_IDP + idpUuid);
    }

    @Test
    void requireExistsThrowsWhenTheProfileIsMissing() {
        var id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.requireExists(id))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage(UserProfileConstant.NOT_FOUND + id);
    }

    @Test
    void deleteRemovesTheStoredProfile() {
        var id = UUID.randomUUID();
        var entity = stored(id, UUID.randomUUID(), "ada@example.com", "Ada", "Lovelace");
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.deleteById(id);

        verify(repository).delete(entity);
        verify(repository).flush();
    }

    private static PlatformTransactionManager directTransactions() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }

    private static UserProfileRequestDto request(UUID idpUuid, String email, String firstName, String lastName) {
        return new UserProfileRequestDto(idpUuid, email, firstName, lastName);
    }

    private static UserProfile stored(UUID id, UUID idpUuid, String email, String firstName, String lastName) {
        var entity = new UserProfile();
        entity.setId(id);
        entity.setIdpUuid(idpUuid);
        entity.setEmail(email);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        return entity;
    }
}
