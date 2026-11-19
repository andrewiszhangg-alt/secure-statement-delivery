package com.chitsa.security.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

/** Matches an Authorization header that uses the bearer scheme, including a malformed token. */
public class BearerAuthorizationRequestMatcher implements RequestMatcher {

    private final DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();

    @Override
    public boolean matches(HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.startsWithIgnoreCase(authorization, SecurityConstant.BEARER)) {
            return false;
        }
        try {
            return resolver.resolve(request) != null;
        } catch (OAuth2AuthenticationException ex) {
            return true;
        }
    }
}
