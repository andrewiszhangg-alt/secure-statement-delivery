package com.chitsa.security.handler;

import com.chitsa.security.util.SecurityConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Logs why the Cognito sign-in failed, then sends the browser back to the sign-in page.
 * Details stay in the log; the page only shows a generic message.
 */
@Slf4j
public class LoggingAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof OAuth2AuthenticationException oauth) {
            var error = oauth.getError();
            log.warn("Sign-in failed: code={} description={} message={}",
                    error.getErrorCode(), error.getDescription(), exception.getMessage());
        } else {
            log.warn("Sign-in failed: {}", exception.getMessage());
        }
        if (exception.getCause() != null) {
            log.warn("Sign-in failure cause: {}", exception.getCause().toString());
        }
        response.sendRedirect(request.getContextPath() + SecurityConstant.LOGIN_FAILURE_PATH);
    }
}
