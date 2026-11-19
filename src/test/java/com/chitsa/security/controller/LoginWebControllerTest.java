package com.chitsa.security.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LoginWebControllerTest {

    private final LoginWebController controller = new LoginWebController();

    @Test
    void showsTheLoginPage() {
        var model = new ExtendedModelMap();

        assertThat(controller.login(null, Map.of(), model)).isEqualTo("login");
        assertThat(model.getAttribute("error")).isEqualTo(false);
    }

    @Test
    void showsAnErrorAfterAFailedSignIn() {
        var model = new ExtendedModelMap();

        assertThat(controller.login(null, Map.of("error", ""), model)).isEqualTo("login");
        assertThat(model.getAttribute("error")).isEqualTo(true);
    }

    @Test
    void sendsASignedInUserToTheirAccounts() {
        assertThat(controller.login(mock(OidcUser.class), Map.of(), new ExtendedModelMap()))
                .isEqualTo("redirect:/accounts");
    }
}
