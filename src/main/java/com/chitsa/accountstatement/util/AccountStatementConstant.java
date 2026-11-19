package com.chitsa.accountstatement.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountStatementConstant {

    public static final String UPDATE_NOT_SUPPORTED = "AccountStatement update is not supported";

    public static final String PRESIGNED_URL = "presignedUrl";

    public static final String STATEMENTS_VIEW = "statements";

    public static final String ERROR_PAGE = "error";

    public static final String ERROR_MESSAGE = "errorMessage";

    public static final String REQUEST_NOT_NULL = "request must not be null";

    public static final String NOT_FOUND = "Statement not found for account=%s statement=%s";

    public static final String WEB_PATH = "/accounts/{accountId}/statements";

    public static final String API_PATH = "/api/accounts/{accountId}/statements";
}
