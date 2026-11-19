package com.chitsa.security.handler;

import com.chitsa.security.config.CognitoProperties;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHandlerTest {

    @Test
    void logoutRedirectsToTheCognitoLogoutEndpoint() throws Exception {
        var properties = new CognitoProperties(
                "https://issuer.example",
                "https://example.auth.eu-central-1.amazoncognito.com",
                "web-client",
                "secret",
                List.of("api-client"),
                List.of("openid"),
                "http://localhost:8080/");
        var response = new MockHttpServletResponse();

        new CognitoLogoutSuccessHandler(properties).onLogoutSuccess(new MockHttpServletRequest(), response, null);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://example.auth.eu-central-1.amazoncognito.com/logout"
                        + "?client_id=web-client&logout_uri=http://localhost:8080/");
    }

    @Test
    void loginFailureRedirectsToTheLoginPage() throws Exception {
        var request = new MockHttpServletRequest();
        request.setContextPath("/statements");
        var response = new MockHttpServletResponse();

        new LoggingAuthenticationFailureHandler().onAuthenticationFailure(
                request, response, new BadCredentialsException("bad password", new IllegalStateException("cause")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/statements" + SecurityConstant.LOGIN_FAILURE_PATH);
    }

    @Test
    void oauthFailureRedirectsWithoutTheProviderError() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var failure = new OAuth2AuthenticationException(new OAuth2Error("access_denied", "user said no", null));

        new LoggingAuthenticationFailureHandler().onAuthenticationFailure(request, response, failure);

        assertThat(response.getRedirectedUrl()).isEqualTo(SecurityConstant.LOGIN_FAILURE_PATH);
        assertThat(response.getRedirectedUrl()).doesNotContain("access_denied");
    }
}
