package com.chitsa.userprofile.config;

import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.util.UserProfileConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/** Keeps signed-in users with an incomplete profile on the complete-profile form. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    private final CurrentProfileProvider currentProfileProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (currentProfileProvider.profileComplete()) {
            return true;
        }
        log.debug("Profile incomplete, redirecting");
        response.sendRedirect(request.getContextPath() + UserProfileConstant.COMPLETE_PROFILE_PATH);
        return false;
    }
}
