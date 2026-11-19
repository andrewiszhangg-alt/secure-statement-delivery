package com.chitsa.admin;

import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.mediaresource.config.MediaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWebControllerTest {

    @Mock
    private AccountTypeService accountTypeService;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountStatementService accountStatementService;

    private AdminWebController controller;

    @BeforeEach
    void setUp() {
        var media = new MediaProperties();
        media.setAllowedExtensions(List.of("pdf"));
        controller = new AdminWebController(accountTypeService, accountService, accountStatementService, media);
    }

    @Test
    void pageListsTypesAndAccounts() {
        when(accountTypeService.findAll()).thenReturn(List.of());
        when(accountService.findAll()).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertThat(controller.page(model)).isEqualTo(AdminWebController.VIEW);
        assertThat(model.getAttribute("accept")).isEqualTo(".pdf");
    }

    @Test
    void createTypeSavesAndRedirects() {
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.createType("loan", "Loan", redirect)).isEqualTo(AdminWebController.REDIRECT);
        verify(accountTypeService).create("loan", "Loan");
        assertThat(redirect.getFlashAttributes().get(AccountConstant.MESSAGE)).isEqualTo(AdminWebController.TYPE_SAVED);
    }

    @Test
    void createTypeReportsADuplicate() {
        doThrow(new DuplicateResourceException("Account type already exists: LOAN"))
                .when(accountTypeService).create("LOAN", "Loan");
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.createType("LOAN", "Loan", redirect)).isEqualTo(AdminWebController.REDIRECT);
        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR)).isEqualTo("Account type already exists: LOAN");
    }

    @Test
    void createStatementUploadsAPdf() {
        var redirect = new RedirectAttributesModelMap();
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "%PDF".getBytes());

        assertThat(controller.createStatement(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", file, redirect)).isEqualTo(AdminWebController.REDIRECT);

        verify(accountStatementService).create(any());
        assertThat(redirect.getFlashAttributes().get(AccountConstant.MESSAGE)).isEqualTo(AdminWebController.STATEMENT_SAVED);
    }

    @Test
    void createStatementRejectsABadAccountId() {
        var redirect = new RedirectAttributesModelMap();
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "%PDF".getBytes());

        assertThat(controller.createStatement("nope", file, redirect)).isEqualTo(AdminWebController.REDIRECT);
        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR)).isInstanceOf(String.class);
    }

    @Test
    void createStatementReportsARejectedFile() {
        doThrow(new InvalidParameterException("File contents do not match the declared type"))
                .when(accountStatementService).create(any());
        var redirect = new RedirectAttributesModelMap();
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "nope".getBytes());

        controller.createStatement("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", file, redirect);

        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR))
                .isEqualTo("File contents do not match the declared type");
    }
}
