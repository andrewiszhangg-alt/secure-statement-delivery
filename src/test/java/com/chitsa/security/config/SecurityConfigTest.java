package com.chitsa.security.config;

import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void webSignInKeepsExistingAuthoritiesAndAddsCognitoRoles() {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .subject("user")
                .claim(SecurityConstant.GROUPS, List.of("ADMIN"))
                .build();
        var oidc = new OidcUserAuthority(idToken, new OidcUserInfo(Map.of("sub", "user")));
        var existing = new SimpleGrantedAuthority("SCOPE_openid");

        var mapped = new SecurityConfig().cognitoAuthoritiesMapper().mapAuthorities(List.of(existing, oidc));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_openid", "OIDC_USER", SecurityConstant.ADMIN_ROLE);
    }

    @Test
    void unknownClientRegistrationIsAbsent() {
        var properties = new CognitoProperties(
                "https://issuer.example", "https://example.auth", "web", "secret",
                List.of("api"), List.of("openid"), "http://localhost:8080/");

        var registration = new CognitoClientRegistrationRepository(properties).findByRegistrationId("google");

        assertThat(registration).isNull();
    }
}
