package com.chitsa.security.config;

import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SecurityFilterChainTest.ProbeController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        HandlerInterceptor.class, WebMvcConfigurer.class, jakarta.servlet.Filter.class
                })
        })
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityFilterChainTest.ProbeController.class})
@TestPropertySource(properties = {
        "app.cognito.issuer-uri=https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_test",
        "app.cognito.domain=https://example.auth.eu-central-1.amazoncognito.com",
        "app.cognito.web-client-id=web-client",
        "app.cognito.web-client-secret=secret",
        "app.cognito.api-client-ids=api-client",
        "app.cognito.scopes=openid,email",
        "app.cognito.logout-redirect-uri=http://localhost:8080/"
})
class SecurityFilterChainTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousApiCallIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/home").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousJsonCallIsUnauthorized() throws Exception {
        mockMvc.perform(get("/home").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerSessionCannotCallTheApi() throws Exception {
        mockMvc.perform(get("/api/probe").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSessionPostRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/probe").with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSessionPostWithCsrfReachesTheController() throws Exception {
        mockMvc.perform(post("/api/probe")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void adminBearerPostSkipsCsrf() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(token(List.of("ADMIN")));

        mockMvc.perform(post("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void customerBearerTokenIsForbidden() throws Exception {
        when(jwtDecoder.decode("customer-token")).thenReturn(token(List.of("USER")));

        mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectedBearerTokenIsUnauthorized() throws Exception {
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("rejected"));

        mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedBearerDoesNotFallBackToTheAdminSession() throws Exception {
        mockMvc.perform(post("/api/probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority(SecurityConstant.ADMIN_ROLE))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotOpenAdmin() throws Exception {
        mockMvc.perform(get("/admin").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousMetricsRedirectToLogin() throws Exception {
        mockMvc.perform(get("/actuator/metrics").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousLivenessIsNotSentToLogin() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isNotFound());
    }

    @Test
    void signedInCustomerCanOpenAPage() throws Exception {
        mockMvc.perform(get("/home").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    private static Jwt token(List<String> groups) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .subject("11111111-1111-1111-1111-111111111111")
                .claim(SecurityConstant.GROUPS, groups)
                .build();
    }

    @RestController
    public static class ProbeController {

        @GetMapping("/api/probe")
        String apiGet() {
            return "ok";
        }

        @PostMapping("/api/probe")
        String apiPost() {
            return "ok";
        }

        @GetMapping("/home")
        String home() {
            return "ok";
        }
    }
}
