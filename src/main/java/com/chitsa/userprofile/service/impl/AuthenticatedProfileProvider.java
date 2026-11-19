package com.chitsa.userprofile.service.impl;

import com.chitsa.common.util.ValidationUtil;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/** Maps the signed-in Cognito user to a profile; names come from the ID token when Cognito provides them. */
@Component
@RequiredArgsConstructor
public class AuthenticatedProfileProvider implements CurrentProfileProvider {

    static final String REQUEST_PROFILE = AuthenticatedProfileProvider.class.getName() + ".profile";

    private final CurrentUser currentUser;
    private final UserProfileService userProfileService;

    @Override
    public UUID profileId() {
        return resolve().id();
    }

    @Override
    public boolean profileComplete() {
        return resolve().profileComplete();
    }

    /** One lookup per HTTP request. Outside a request, every call reads again. */
    private UserProfileResponseDto resolve() {
        var request = currentRequest();
        if (request != null) {
            var cached = request.getAttribute(REQUEST_PROFILE);
            if (cached instanceof UserProfileResponseDto dto) {
                return dto;
            }
        }
        var loaded = load();
        if (request != null) {
            request.setAttribute(REQUEST_PROFILE, loaded);
        }
        return loaded;
    }

    private UserProfileResponseDto load() {
        var email = currentUser.email();
        ValidationUtil.requireNotBlank(email, UserProfileConstant.MISSING_EMAIL);
        var request = new UserProfileRequestDto(
                currentUser.idpUuid(), email, currentUser.firstName(), currentUser.lastName());
        return userProfileService.findOrCreateByIdpUuid(request);
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
