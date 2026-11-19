package com.chitsa.security.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Reads Cognito-specific claims. */
@UtilityClass
public class CognitoClaims {

    /** Each Cognito group becomes a role: group {@code ADMIN} gives {@code ROLE_ADMIN}. */
    public static Collection<GrantedAuthority> authorities(Map<String, Object> claims) {
        if (claims.get(SecurityConstant.GROUPS) instanceof List<?> groups) {
            return groups.stream()
                    .map(group -> (GrantedAuthority) new SimpleGrantedAuthority(SecurityConstant.ROLE_PREFIX + group))
                    .toList();
        }
        return List.of();
    }
}
