package com.chitsa.security.service;

import java.util.UUID;

/** The signed-in caller, whether authenticated by web session or by bearer token. */
public interface CurrentUser {

    /** Cognito {@code sub}. */
    UUID idpUuid();

    /** Email claim; only present on web sign-in (ID token). */
    String email();

    String firstName();

    String lastName();

    boolean isAdmin();
}
