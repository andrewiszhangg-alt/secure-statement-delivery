package com.chitsa.security.util;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Cognito access tokens carry no audience, so the token type and the app client are checked instead. */
public class AccessTokenClientValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> clientIds;

    public AccessTokenClientValidator(List<String> clientIds) {
        this.clientIds = clientIds;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var accessToken = SecurityConstant.ACCESS_TOKEN.equals(jwt.getClaimAsString(SecurityConstant.TOKEN_USE));
        var knownClient = clientIds != null && clientIds.contains(jwt.getClaimAsString(SecurityConstant.CLIENT_ID));
        if (accessToken && knownClient) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(SecurityConstant.INVALID_TOKEN, SecurityConstant.NOT_AN_ACCESS_TOKEN, null));
    }
}
