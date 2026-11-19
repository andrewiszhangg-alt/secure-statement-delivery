package com.chitsa.accountstatement.controller;

import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.security.config.SecurityConfig;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AccountStatementController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        HandlerInterceptor.class, WebMvcConfigurer.class, jakarta.servlet.Filter.class
                })
        })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cognito.issuer-uri=https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_test",
        "app.cognito.domain=https://example.auth.eu-central-1.amazoncognito.com",
        "app.cognito.web-client-id=web-client",
        "app.cognito.web-client-secret=secret",
        "app.cognito.api-client-ids=api-client",
        "app.cognito.scopes=openid,email",
        "app.cognito.logout-redirect-uri=http://localhost:8080/"
})
class AccountStatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountStatementService accountStatementService;

    private final UUID accountId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID statementId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        mockMvc.perform(get(path()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get(path())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReceivesThePage() throws Exception {
        when(accountStatementService.findByAccountId(accountId, 0)).thenReturn(new PageImpl<>(List.of(
                AccountStatementResponseDto.builder().id(statementId).build())));

        mockMvc.perform(get(path())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(statementId.toString()));
    }

    @Test
    void adminCreateReturnsCreated() throws Exception {
        when(accountStatementService.create(any())).thenReturn(
                AccountStatementResponseDto.builder().id(statementId).build());
        var file = new MockMultipartFile("file", "statement.pdf", MediaType.APPLICATION_PDF_VALUE, "statement".getBytes());

        mockMvc.perform(multipart(path())
                        .file(file)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(statementId.toString()));
    }

    @Test
    void adminReceivesThePresignedUrl() throws Exception {
        when(accountStatementService.generatePresignedUrl(accountId, statementId))
                .thenReturn(Map.of(AccountStatementConstant.PRESIGNED_URL, "https://signed.example/file"));

        mockMvc.perform(get(path() + "/" + statementId + "/presigned-url")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").value("https://signed.example/file"));
    }

    @Test
    void adminDeleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(path() + "/" + statementId)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"));
    }

    private String path() {
        return AccountStatementConstant.API_PATH.replace("{accountId}", accountId.toString());
    }
}
