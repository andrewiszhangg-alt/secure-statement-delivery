package com.chitsa.security.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenClientValidatorTest {

    private final AccessTokenClientValidator validator = new AccessTokenClientValidator(List.of("api-client"));

    @Test
    void acceptsAnAccessTokenFromAKnownClient() {
        var result = validator.validate(jwt(SecurityConstant.ACCESS_TOKEN, "api-client"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsAnIdToken() {
        var result = validator.validate(jwt("id", "api-client"));

        assertThat(result.getErrors()).extracting(error -> error.getDescription())
                .containsExactly("Not an access token from an allowed app client");
    }

    @Test
    void rejectsAnUnknownClient() {
        var result = validator.validate(jwt(SecurityConstant.ACCESS_TOKEN, "other-client"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsWhenNoClientsAreConfigured() {
        var result = new AccessTokenClientValidator(null).validate(jwt(SecurityConstant.ACCESS_TOKEN, "api-client"));

        assertThat(result.hasErrors()).isTrue();
    }

    private static Jwt jwt(String tokenUse, String clientId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .subject("user")
                .claim(SecurityConstant.TOKEN_USE, tokenUse)
                .claim(SecurityConstant.CLIENT_ID, clientId)
                .build();
    }
}
