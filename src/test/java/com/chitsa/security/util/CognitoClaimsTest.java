package com.chitsa.security.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoClaimsTest {

    @Test
    void mapsEachGroupToARole() {
        var authorities = CognitoClaims.authorities(Map.of(SecurityConstant.GROUPS, List.of("ADMIN", "USER")));

        assertThat(authorities).extracting(authority -> authority.getAuthority())
                .containsExactly(SecurityConstant.ADMIN_ROLE, "ROLE_USER");
    }

    @Test
    void returnsNothingWhenGroupsAreMissing() {
        assertThat(CognitoClaims.authorities(Map.of())).isEmpty();
    }

    @Test
    void returnsNothingWhenGroupsAreNotAList() {
        assertThat(CognitoClaims.authorities(Map.of(SecurityConstant.GROUPS, "ADMIN"))).isEmpty();
    }
}
