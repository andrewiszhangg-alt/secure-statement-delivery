package com.chitsa.userprofile.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserProfileConstant {

    public static final String COMPLETE_PROFILE_VIEW = "complete-profile";

    public static final String COMPLETE_PROFILE_PATH = "/complete-profile";

    public static final String ACCOUNTS_REDIRECT = "redirect:/accounts";

    public static final String NAMES_REQUIRED = "Enter your first and last name (up to 255 characters each)";

    public static final String PROFILE_SAVED = "Profile saved";

    public static final String PROFILE_INCOMPLETE =
            "Complete your profile (first name, last name, email) before continuing";

    public static final String MISSING_EMAIL =
            "Your sign-in has no email address; add the email scope to the app client";

    public static final String EMAIL = "email";

    public static final String FIRST_NAME = "firstName";

    public static final String LAST_NAME = "lastName";

    public static final String ERROR = "error";

    public static final String MESSAGE = "message";

    public static final String REQUEST_NOT_NULL = "request must not be null";

    public static final String DUPLICATE = "Profile already exists for idpUuid: ";

    public static final String NOT_FOUND = "Profile not found: ";

    public static final String NOT_FOUND_FOR_IDP = "Profile not found for idpUuid: ";

    public static final String API_PATH = "/api/profiles";
}
