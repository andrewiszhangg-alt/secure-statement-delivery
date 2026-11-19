package com.chitsa.accountstatement.service.impl;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.service.AccountService;
import com.chitsa.accountstatement.config.AccountStatementProperties;
import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.entity.AccountStatement;
import com.chitsa.accountstatement.exception.AccountStatementNotFoundException;
import com.chitsa.accountstatement.mapper.AccountStatementMapper;
import com.chitsa.accountstatement.repository.AccountStatementRepository;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.accountstatement.validator.AccountStatementValidator;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.dto.MediaResourceResponseDto;
import com.chitsa.mediaresource.service.MediaResourceService;
import com.chitsa.userprofile.service.UserProfileService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatementServiceImplTest {

    @Mock
    private AccountStatementRepository repository;

    @Mock
    private AccountService accountService;

    @Mock
    private MediaResourceService mediaResourceService;

    @Mock
    private UserProfileService userProfileService;

    private AccountStatementProperties properties;

    private AccountStatementServiceImpl service;

    private final UUID accountId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID profileId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID mediaId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final UUID statementId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @BeforeEach
    void setUp() {
        properties = new AccountStatementProperties();
        var validator = new AccountStatementValidator(
                new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        service = new AccountStatementServiceImpl(
                repository, new AccountStatementMapper(), validator, properties,
                accountService, mediaResourceService, userProfileService);
    }

    @Test
    void createChecksTheAccountAndProfileBeforeUploading() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(mediaResourceService.create(any())).thenReturn(MediaResourceResponseDto.builder().id(mediaId).build());
        when(repository.save(any())).thenAnswer(invocation -> {
            AccountStatement entity = invocation.getArgument(0);
            entity.setId(statementId);
            return entity;
        });

        var response = service.create(request("statement.pdf"));

        InOrder order = inOrder(accountService, userProfileService, mediaResourceService, repository);
        order.verify(accountService).findById(accountId);
        order.verify(userProfileService).requireCompleteProfile(profileId);
        order.verify(mediaResourceService).create(any());
        order.verify(repository).save(any());
        assertThat(response.id()).isEqualTo(statementId);
    }

    @Test
    void createStoresTheFileUnderTheAccount() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(mediaResourceService.create(any())).thenReturn(MediaResourceResponseDto.builder().id(mediaId).build());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request("statement.final.pdf"));

        var media = ArgumentCaptor.forClass(MediaResourceRequestDto.class);
        verify(mediaResourceService).create(media.capture());
        assertThat(media.getValue().context()).isEqualTo("account-statements");
        var key = media.getValue().objectKey();
        assertThat(key).startsWith(accountId + "/").endsWith(".pdf");
        var generated = key.substring((accountId + "/").length(), key.length() - ".pdf".length());
        assertThat(UUID.fromString(generated)).isNotNull();

        var saved = ArgumentCaptor.forClass(AccountStatement.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAccountId()).isEqualTo(accountId);
        assertThat(saved.getValue().getMediaResourceId()).isEqualTo(mediaId);
    }

    @Test
    void createOmitsTheExtensionWhenTheFileHasNone() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(mediaResourceService.create(any())).thenReturn(MediaResourceResponseDto.builder().id(mediaId).build());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request("statement"));

        var media = ArgumentCaptor.forClass(MediaResourceRequestDto.class);
        verify(mediaResourceService).create(media.capture());
        var generated = media.getValue().objectKey().substring((accountId + "/").length());
        assertThat(generated).doesNotContain(".");
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    void createStopsWhenTheAccountIsMissing() {
        when(accountService.findById(accountId)).thenThrow(new AccountNotFoundException("missing"));

        assertThatThrownBy(() -> service.create(request("statement.pdf")))
                .isInstanceOf(AccountNotFoundException.class);
        verify(userProfileService, never()).requireCompleteProfile(any());
        verify(mediaResourceService, never()).create(any());
    }

    @Test
    void createStopsWhenTheProfileIsIncomplete() {
        when(accountService.findById(accountId)).thenReturn(account());
        doThrow(new InvalidParameterException("incomplete")).when(userProfileService).requireCompleteProfile(profileId);

        assertThatThrownBy(() -> service.create(request("statement.pdf")))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("incomplete");
        verify(mediaResourceService, never()).create(any());
    }

    @Test
    void listClampsThePageAndReadsNewestFirst() {
        var newer = statement(statementId, Instant.parse("2026-02-01T00:00:00Z"));
        var older = statement(UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
        when(accountService.findById(accountId)).thenReturn(account());
        when(repository.findByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newer, older)));
        properties.setPageSize(0);

        var page = service.findByAccountId(accountId, -4);

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByAccountId(eq(accountId), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
        assertThat(pageable.getValue().getSort().getOrderFor("createTime").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(page.getContent()).extracting(AccountStatementResponseDto::id)
                .containsExactly(statementId, older.getId());
    }

    @Test
    void presignReturnsTheLinkForAStatementOnTheAccount() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(repository.findByIdAndAccountId(statementId, accountId)).thenReturn(Optional.of(statement(statementId, Instant.now())));
        when(mediaResourceService.generatePreSignedUrlForResource(mediaId)).thenReturn("https://signed.example/file");

        var link = service.generatePresignedUrl(accountId, statementId);

        assertThat(link).containsEntry(AccountStatementConstant.PRESIGNED_URL, "https://signed.example/file");
    }

    @Test
    void aStatementFromAnotherAccountIsNotFound() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(repository.findByIdAndAccountId(statementId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePresignedUrl(accountId, statementId))
                .isInstanceOf(AccountStatementNotFoundException.class)
                .hasMessage(AccountStatementConstant.NOT_FOUND.formatted(accountId, statementId));
        verify(mediaResourceService, never()).generatePreSignedUrlForResource(any());
    }

    @Test
    void deleteRemovesTheLinkThenTheMedia() {
        var entity = statement(statementId, Instant.now());
        when(accountService.findById(accountId)).thenReturn(account());
        when(repository.findByIdAndAccountId(statementId, accountId)).thenReturn(Optional.of(entity));

        service.deleteById(accountId, statementId);

        InOrder order = inOrder(repository, mediaResourceService);
        order.verify(repository).delete(entity);
        order.verify(repository).flush();
        order.verify(mediaResourceService).deleteById(mediaId);
    }

    @Test
    void deleteLeavesAForeignStatementInPlace() {
        when(accountService.findById(accountId)).thenReturn(account());
        when(repository.findByIdAndAccountId(statementId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(accountId, statementId))
                .isInstanceOf(AccountStatementNotFoundException.class);
        verify(repository, never()).delete(any());
        verify(mediaResourceService, never()).deleteById(any());
    }

    private AccountResponseDto account() {
        return AccountResponseDto.builder().id(accountId).userProfileId(profileId).build();
    }

    private AccountStatement statement(UUID id, Instant created) {
        var entity = new AccountStatement();
        entity.setId(id);
        entity.setAccountId(accountId);
        entity.setMediaResourceId(mediaId);
        entity.setCreateTime(created);
        entity.setUpdatedTime(created);
        return entity;
    }

    private AccountStatementRequestDto request(String filename) {
        MultipartFile file = new MockMultipartFile("file", filename, "application/pdf", "statement".getBytes());
        return new AccountStatementRequestDto(accountId, file);
    }
}
