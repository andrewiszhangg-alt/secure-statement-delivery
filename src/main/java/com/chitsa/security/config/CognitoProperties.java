package com.chitsa.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Cognito settings. Values are placeholders until the user pool exists.
 *
 * @param issuerUri         {@code https://cognito-idp.<region>.amazonaws.com/<userPoolId>}
 * @param domain            Hosted UI domain, e.g. {@code https://<prefix>.auth.<region>.amazoncognito.com}
 * @param webClientId       confidential app client used by the web pages
 * @param webClientSecret   secret of that app client
 * @param apiClientIds      app clients whose access tokens may call {@code /api/**}
 * @param scopes            OAuth scopes requested at sign-in; each must be enabled on the app client
 * @param logoutRedirectUri where Cognito sends the browser after sign-out (must be an allowed sign-out URL)
 */
@ConfigurationProperties(prefix = "app.cognito")
public record CognitoProperties(
        String issuerUri,
        String domain,
        String webClientId,
        String webClientSecret,
        List<String> apiClientIds,
        List<String> scopes,
        String logoutRedirectUri) {
}
