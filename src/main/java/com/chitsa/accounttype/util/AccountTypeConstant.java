package com.chitsa.accounttype.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountTypeConstant {

    public static final String NOT_FOUND = "Account type not found: ";

    public static final String CODE_REQUIRED = "Account type code is required";

    public static final String LABEL_REQUIRED = "Account type label is required";

    public static final String CODE_INVALID = "Account type code must be uppercase letters, digits, or underscores";

    public static final String LABEL_INVALID = "Account type label must be 100 characters or fewer";

    public static final String DUPLICATE = "Account type already exists: ";

    public static final String API_PATH = "/api/account-types";
}
