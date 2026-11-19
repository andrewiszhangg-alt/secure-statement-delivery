package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.security.config.SecurityConfig;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AccountRestController.class,
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
class AccountRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        mockMvc.perform(get(AccountConstant.API_PATH + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get(AccountConstant.API_PATH + "/" + UUID.randomUUID())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReceivesTheAccount() throws Exception {
        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(accountService.findById(id)).thenReturn(AccountResponseDto.builder().id(id).build());

        mockMvc.perform(get(AccountConstant.API_PATH + "/" + id)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void adminCreateReturnsCreated() throws Exception {
        var id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(accountService.create(any())).thenReturn(AccountResponseDto.builder().id(id).build());

        mockMvc.perform(post(AccountConstant.API_PATH)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userProfileId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","accountTypeId":"cccccccc-cccc-cccc-cccc-cccccccccccc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void adminUpdateReturnsTheAccount() throws Exception {
        var id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(accountService.update(eq(id), any()))
                .thenReturn(AccountResponseDto.builder().id(id).build());

        mockMvc.perform(put(AccountConstant.API_PATH + "/" + id)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountTypeId":"cccccccc-cccc-cccc-cccc-cccccccccccc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void adminListReturnsTheAccounts() throws Exception {
        var owner = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(accountService.findByUserProfileId(owner)).thenReturn(List.of(
                AccountResponseDto.builder().id(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")).build()));

        mockMvc.perform(get(AccountConstant.API_PATH)
                        .param("userProfileId", owner.toString())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    @Test
    void adminDeleteReturnsNoContent() throws Exception {
        var id = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(delete(AccountConstant.API_PATH + "/" + id)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
