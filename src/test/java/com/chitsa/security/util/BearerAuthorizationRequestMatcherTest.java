package com.chitsa.security.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class BearerAuthorizationRequestMatcherTest {

    private final BearerAuthorizationRequestMatcher matcher = new BearerAuthorizationRequestMatcher();

    @Test
    void matchesABearerToken() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token");

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchesABearerSchemeInAnyCase() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer admin-token");

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void rejectsAMissingHeader() {
        assertThat(matcher.matches(new MockHttpServletRequest())).isFalse();
    }

    @Test
    void rejectsABasicCredential() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void treatsAMalformedBearerHeaderAsABearerCall() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");

        assertThat(matcher.matches(request)).isTrue();
    }
}
