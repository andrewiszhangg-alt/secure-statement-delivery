package com.chitsa.accounttype.controller;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.accounttype.util.AccountTypeConstant;
import com.chitsa.security.config.SecurityConfig;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AccountTypeRestController.class,
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
class AccountTypeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountTypeService accountTypeService;

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        mockMvc.perform(get(AccountTypeConstant.API_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get(AccountTypeConstant.API_PATH)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReceivesTheCatalog() throws Exception {
        var id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(accountTypeService.findAll()).thenReturn(List.of(
                AccountTypeResponseDto.builder().id(id).code("SAVINGS").label("Savings").build()));

        mockMvc.perform(get(AccountTypeConstant.API_PATH)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].code").value("SAVINGS"))
                .andExpect(jsonPath("$[0].label").value("Savings"));
    }
}
