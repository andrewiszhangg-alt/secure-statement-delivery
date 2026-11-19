package com.chitsa.userprofile.service;

import java.util.UUID;

/** Resolves the profile of the signed-in user, creating it on first sign-in. */
public interface CurrentProfileProvider {

    UUID profileId();

    /** True when the signed-in profile has an email and both names. Resolved with {@link #profileId()}. */
    boolean profileComplete();
}
