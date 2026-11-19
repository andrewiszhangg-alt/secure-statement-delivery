package com.chitsa.security.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityConstant {

    public static final String ACCESS_DENIED = "You are not allowed to access this resource";

    public static final String ADMINS_ONLY = "Admins only";

    public static final String INVALID_SUBJECT = "Signed-in user has no valid subject";

    public static final String NO_CLAIMS = "Authenticated principal has no claims";

    public static final String NOT_AUTHENTICATED = "Not authenticated";

    public static final String NOT_AN_ACCESS_TOKEN = "Not an access token from an allowed app client";

    public static final String COGNITO_NOT_CONFIGURED = "Cognito is not configured; set the app.cognito.* properties";

    public static final String INVALID_TOKEN = "invalid_token";

    public static final String DEV_TOKEN_ENABLED = "app.security.dev-token.enabled";

    public static final String DEV_TOKEN_PATH = "/dev/access-token";

    public static final String ADMIN = "ADMIN";

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String ADMIN_ROLE = ROLE_PREFIX + ADMIN;

    public static final String REGISTRATION_ID = "cognito";

    public static final String BEARER = "bearer";

    public static final String JWT = "JWT";

    public static final String BEARER_AUTH = "bearerAuth";

    public static final String CSRF_AUTH = "csrfToken";

    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    public static final String CSRF_HEADER_DESCRIPTION =
            "Value of the XSRF-TOKEN cookie (set after any GET to /api)";

    public static final String API_PATH = "/api/**";

    public static final String LOGIN_PATH = "/login";

    public static final String LOGIN_VIEW = "login";

    public static final String LOGIN_ERROR = "error";

    public static final String LOGIN_FAILURE_PATH = LOGIN_PATH + "?" + LOGIN_ERROR;

    public static final String OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization";

    public static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";

    public static final String LOGOUT_PATH = "/logout";

    public static final String LOGOUT_URI = "logout_uri";

    public static final String ACCOUNTS_REDIRECT = "redirect:/accounts";

    public static final String GROUPS = "cognito:groups";

    public static final String TOKEN_USE = "token_use";

    public static final String ACCESS_TOKEN = "access";

    public static final String CLIENT_ID = "client_id";

    public static final String SUBJECT = "sub";

    public static final String EMAIL = "email";

    public static final String GIVEN_NAME = "given_name";

    public static final String FAMILY_NAME = "family_name";

    public static final String[] PUBLIC_PATHS = {
            "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", LOGIN_PATH
    };

    public static final String[] DOC_PATHS = {"/scalar/**", "/v3/api-docs/**"};
}
