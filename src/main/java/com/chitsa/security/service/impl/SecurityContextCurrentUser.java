package com.chitsa.security.service.impl;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.security.util.SecurityConstant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Reads the caller from the security context; the principal is an OIDC user (web) or a JWT (API). */
@Component
public class SecurityContextCurrentUser implements CurrentUser {

    @Override
    public UUID idpUuid() {
        var sub = claim(SecurityConstant.SUBJECT);
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidParameterException(SecurityConstant.INVALID_SUBJECT);
        }
    }

    @Override
    public String email() {
        return claim(SecurityConstant.EMAIL);
    }

    @Override
    public String firstName() {
        return claim(SecurityConstant.GIVEN_NAME);
    }

    @Override
    public String lastName() {
        return claim(SecurityConstant.FAMILY_NAME);
    }

    @Override
    public boolean isAdmin() {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> SecurityConstant.ADMIN_ROLE.equals(authority.getAuthority()));
    }

    private String claim(String name) {
        return claims().get(name) instanceof String value ? value : null;
    }

    private Map<String, Object> claims() {
        if (authentication().getPrincipal() instanceof ClaimAccessor accessor) {
            return accessor.getClaims();
        }
        throw new AccessDeniedException(SecurityConstant.NO_CLAIMS);
    }

    private Authentication authentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(SecurityConstant.NOT_AUTHENTICATED);
        }
        return authentication;
    }
}
