package com.chitsa.security.controller;

import com.chitsa.security.service.CurrentUser;
import com.chitsa.security.util.SecurityConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the signed-in admin's Cognito access token.
 */
@ConditionalOnProperty(name = SecurityConstant.DEV_TOKEN_ENABLED, havingValue = "true")
@RestController
@RequiredArgsConstructor
public class LocalTokenController {

    private final CurrentUser currentUser;

    @GetMapping(value = SecurityConstant.DEV_TOKEN_PATH, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> accessToken(
            @RegisteredOAuth2AuthorizedClient(SecurityConstant.REGISTRATION_ID) OAuth2AuthorizedClient client) {
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(SecurityConstant.ADMINS_ONLY);
        }
        return ResponseEntity.ok(client.getAccessToken().getTokenValue());
    }
}
