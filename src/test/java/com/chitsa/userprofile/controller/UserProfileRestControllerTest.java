package com.chitsa.userprofile.controller;

import com.chitsa.security.config.SecurityConfig;
import com.chitsa.security.util.SecurityConstant;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserProfileRestController.class,
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
class UserProfileRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        mockMvc.perform(get(UserProfileConstant.API_PATH + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get(UserProfileConstant.API_PATH + "/" + UUID.randomUUID())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateReturnsTheProfile() throws Exception {
        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(userProfileService.create(any())).thenReturn(
                UserProfileResponseDto.builder().id(id).email("ada@example.com").firstName("Ada").lastName("Lovelace").build());

        mockMvc.perform(post(UserProfileConstant.API_PATH)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idpUuid":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","email":"ada@example.com","firstName":"Ada","lastName":"Lovelace"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }
}
