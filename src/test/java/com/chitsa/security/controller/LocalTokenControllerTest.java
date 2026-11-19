package com.chitsa.security.controller;

import com.chitsa.security.service.CurrentUser;
import com.chitsa.security.util.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalTokenControllerTest {

    @Test
    void staysUnregisteredUnlessTheFlagIsOn() {
        context().run(ctx -> assertThat(ctx).doesNotHaveBean(LocalTokenController.class));
    }

    @Test
    void registersWhenTheFlagIsOn() {
        context().withPropertyValues(SecurityConstant.DEV_TOKEN_ENABLED + "=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(LocalTokenController.class));
    }

    @Test
    void returnsTheAdminsOwnAccessToken() {
        var user = mock(CurrentUser.class);
        when(user.isAdmin()).thenReturn(true);
        var client = clientWithToken("access-token");

        var response = new LocalTokenController(user).accessToken(client);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("access-token");
    }

    @Test
    void rejectsACallerWhoIsNotAnAdmin() {
        var user = mock(CurrentUser.class);
        when(user.isAdmin()).thenReturn(false);

        var response = new LocalTokenController(user).accessToken(clientWithToken("access-token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(SecurityConstant.ADMINS_ONLY);
    }

    private static WebApplicationContextRunner context() {
        return new WebApplicationContextRunner()
                .withUserConfiguration(LocalTokenController.class)
                .withBean(CurrentUser.class, () -> mock(CurrentUser.class));
    }

    private static OAuth2AuthorizedClient clientWithToken(String value) {
        var client = mock(OAuth2AuthorizedClient.class);
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, Instant.now(), Instant.now().plusSeconds(60));
        when(client.getAccessToken()).thenReturn(token);
        return client;
    }
}
