package com.chitsa.accountstatement.controller;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.common.exception.EntityNotFoundException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.mediaresource.exception.ObjectStorageApiException;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatementWebControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountStatementService accountStatementService;

    @InjectMocks
    private AccountStatementWebController controller;

    private final UUID accountId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID statementId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "linkMinutes", 15);
    }

    @Test
    void listShowsTheAccountPage() {
        var account = AccountResponseDto.builder().id(accountId).build();
        var statements = new PageImpl<>(java.util.List.of(AccountStatementResponseDto.builder().id(statementId).build()));
        when(accountService.findById(accountId)).thenReturn(account);
        when(accountStatementService.findByAccountId(accountId, 0)).thenReturn(statements);
        var model = new ExtendedModelMap();

        assertThat(controller.list(accountId, -2, model)).isEqualTo(AccountStatementConstant.STATEMENTS_VIEW);
        assertThat(model.get("currentAccount")).isEqualTo(account);
        assertThat(model.get("statements")).isEqualTo(statements);
        assertThat(model.get("linkMinutes")).isEqualTo(15);
        verify(accountStatementService).findByAccountId(accountId, 0);
    }

    @Test
    void linkReturnsThePresignedUrl() {
        when(accountStatementService.generatePresignedUrl(accountId, statementId))
                .thenReturn(Map.of(AccountStatementConstant.PRESIGNED_URL, "https://signed.example/file"));

        assertThat(controller.link(accountId, statementId))
                .containsEntry(AccountStatementConstant.PRESIGNED_URL, "https://signed.example/file");
    }

    @Test
    void viewRedirectsWithoutCachingTheLink() {
        when(accountStatementService.generatePresignedUrl(accountId, statementId))
                .thenReturn(Map.of(AccountStatementConstant.PRESIGNED_URL, "https://signed.example/file"));

        var response = controller.view(accountId, statementId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("https://signed.example/file"));
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}

class WebExceptionHandlerTest {

    private final WebExceptionHandler handler = new WebExceptionHandler();

    @Test
    void notFoundShowsTheMessage() {
        var model = new ExtendedModelMap();

        assertThat(handler.handleNotFound(new EntityNotFoundException("missing statement"), model))
                .isEqualTo(AccountStatementConstant.ERROR_PAGE);
        assertThat(model.get(AccountStatementConstant.ERROR_MESSAGE)).isEqualTo("missing statement");
    }

    @Test
    void invalidRequestShowsTheMessage() {
        var model = new ExtendedModelMap();

        assertThat(handler.handleInvalid(new InvalidParameterException("choose a file"), model))
                .isEqualTo(AccountStatementConstant.ERROR_PAGE);
        assertThat(model.get(AccountStatementConstant.ERROR_MESSAGE)).isEqualTo("choose a file");
    }

    @Test
    void storageFailureShowsTheFixedSentence() {
        var model = new ExtendedModelMap();

        assertThat(handler.handleStorage(new ObjectStorageApiException("bucket secret"), model))
                .isEqualTo(AccountStatementConstant.ERROR_PAGE);
        assertThat(model.get(AccountStatementConstant.ERROR_MESSAGE)).isEqualTo(MediaResourceConstant.STORAGE_UNAVAILABLE);
    }

    @Test
    void optimisticLockShowsTheConflictSentence() {
        var model = new ExtendedModelMap();

        assertThat(handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("AccountStatement", UUID.randomUUID()), model))
                .isEqualTo(AccountStatementConstant.ERROR_PAGE);
        assertThat(model.get(AccountStatementConstant.ERROR_MESSAGE)).isEqualTo(PersistenceConstant.VERSION_CONFLICT);
    }
}
