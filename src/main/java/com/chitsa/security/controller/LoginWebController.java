package com.chitsa.security.controller;

import com.chitsa.security.util.SecurityConstant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/** Public sign-in page; its single button starts the Cognito login. Also shows a failed sign-in (?error). */
@Controller
public class LoginWebController {

    @GetMapping(SecurityConstant.LOGIN_PATH)
    public String login(@AuthenticationPrincipal OidcUser user, @RequestParam Map<String, String> params,
                        Model model) {
        if (user != null) {
            return SecurityConstant.ACCOUNTS_REDIRECT;
        }
        model.addAttribute(SecurityConstant.LOGIN_ERROR, params.containsKey(SecurityConstant.LOGIN_ERROR));
        return SecurityConstant.LOGIN_VIEW;
    }
}
