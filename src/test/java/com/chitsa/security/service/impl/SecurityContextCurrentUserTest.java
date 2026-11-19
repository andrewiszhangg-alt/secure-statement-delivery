package com.chitsa.security.service.impl;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentUserTest {

    private final SecurityContextCurrentUser currentUser = new SecurityContextCurrentUser();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsClaimsFromTheSignedInPrincipal() {
        var id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        authenticate(jwt(id.toString(), "ada@example.com"), SecurityConstant.ADMIN_ROLE);

        assertThat(currentUser.idpUuid()).isEqualTo(id);
        assertThat(currentUser.email()).isEqualTo("ada@example.com");
        assertThat(currentUser.firstName()).isEqualTo("Ada");
        assertThat(currentUser.lastName()).isEqualTo("Lovelace");
        assertThat(currentUser.isAdmin()).isTrue();
    }

    @Test
    void returnsNullForAClaimThatIsNotText() {
        var jwt = jwt("11111111-1111-1111-1111-111111111111", "ada@example.com");
        var withNumber = Jwt.withTokenValue(jwt.getTokenValue())
                .headers(headers -> headers.putAll(jwt.getHeaders()))
                .claims(claims -> claims.putAll(jwt.getClaims()))
                .claim(SecurityConstant.EMAIL, 1)
                .build();
        authenticate(withNumber, SecurityConstant.ADMIN_ROLE);

        assertThat(currentUser.email()).isNull();
    }

    @Test
    void rejectsASubjectThatIsNotAUuid() {
        authenticate(jwt("not-a-uuid", "ada@example.com"), "ROLE_USER");

        assertThatThrownBy(currentUser::idpUuid)
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(SecurityConstant.INVALID_SUBJECT);
    }

    @Test
    void rejectsAMissingSubject() {
        authenticate(jwt(null, "ada@example.com"), "ROLE_USER");

        assertThatThrownBy(currentUser::idpUuid)
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(SecurityConstant.INVALID_SUBJECT);
    }

    @Test
    void isAdminIsFalseForAnotherRole() {
        authenticate(jwt("11111111-1111-1111-1111-111111111111", "ada@example.com"), "ROLE_USER");

        assertThat(currentUser.isAdmin()).isFalse();
    }

    @Test
    void rejectsAnAnonymousCaller() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(currentUser::email)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    void rejectsAMissingAuthentication() {
        assertThatThrownBy(currentUser::email)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    void rejectsAPrincipalWithoutClaims() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ada", "n/a", List.of()));

        assertThatThrownBy(currentUser::email)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authenticated principal has no claims");
    }

    private static void authenticate(Jwt jwt, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role))));
    }

    private static Jwt jwt(String subject, String email) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .claim(SecurityConstant.EMAIL, email)
                .claim(SecurityConstant.GIVEN_NAME, "Ada")
                .claim(SecurityConstant.FAMILY_NAME, "Lovelace");
        if (subject != null) {
            builder.subject(subject);
        }
        return builder.build();
    }
}
