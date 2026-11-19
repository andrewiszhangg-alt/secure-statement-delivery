package com.chitsa.security.handler;

import com.chitsa.security.config.CognitoProperties;
import com.chitsa.security.util.SecurityConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/** Redirects sign-out to the Cognito logout endpoint. */
@RequiredArgsConstructor
public class CognitoLogoutSuccessHandler implements LogoutSuccessHandler {

    private final CognitoProperties properties;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        var url = UriComponentsBuilder.fromUriString(properties.domain())
                .path(SecurityConstant.LOGOUT_PATH)
                .queryParam(SecurityConstant.CLIENT_ID, properties.webClientId())
                .queryParam(SecurityConstant.LOGOUT_URI, properties.logoutRedirectUri())
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(url);
    }
}
