package com.chitsa.userprofile.controller;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.util.UserProfileConstant;
import com.chitsa.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** First-sign-in form that collects the names a profile needs before accounts and statements are available. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserProfileWebController {

    private final UserProfileService userProfileService;
    private final CurrentProfileProvider currentProfileProvider;

    @GetMapping(UserProfileConstant.COMPLETE_PROFILE_PATH)
    public String form(Model model) {
        var profileId = currentProfileProvider.profileId();
        if (userProfileService.isProfileComplete(profileId)) {
            return UserProfileConstant.ACCOUNTS_REDIRECT;
        }
        var profile = userProfileService.findById(profileId);
        model.addAttribute(UserProfileConstant.EMAIL, profile.email());
        // flash values from a failed submit win over what is stored
        keepSubmitted(model, UserProfileConstant.FIRST_NAME, profile.firstName());
        keepSubmitted(model, UserProfileConstant.LAST_NAME, profile.lastName());
        return UserProfileConstant.COMPLETE_PROFILE_VIEW;
    }

    @PostMapping(UserProfileConstant.COMPLETE_PROFILE_PATH)
    public String save(
            @RequestParam String firstName,
            @RequestParam String lastName,
            RedirectAttributes redirectAttributes) {
        var profileId = currentProfileProvider.profileId();
        try {
            userProfileService.update(profileId, UserProfileRequestDto.forUpdate(firstName, lastName));
        } catch (InvalidParameterException ex) {
            return reject(redirectAttributes, profileId, "Profile completion rejected", ex,
                    UserProfileConstant.NAMES_REQUIRED, firstName, lastName);
        } catch (OptimisticLockingFailureException ex) {
            return reject(redirectAttributes, profileId, "Profile update conflict", ex,
                    PersistenceConstant.VERSION_CONFLICT, firstName, lastName);
        }
        redirectAttributes.addFlashAttribute(UserProfileConstant.MESSAGE, UserProfileConstant.PROFILE_SAVED);
        return UserProfileConstant.ACCOUNTS_REDIRECT;
    }

    private static void keepSubmitted(Model model, String name, String stored) {
        if (!model.containsAttribute(name)) {
            model.addAttribute(name, stored);
        }
    }

    private static String reject(
            RedirectAttributes redirectAttributes,
            UUID profileId,
            String event,
            Exception ex,
            String message,
            String firstName,
            String lastName) {
        log.warn("{} profileId={}: {}", event, profileId, ex.getMessage());
        redirectAttributes.addFlashAttribute(UserProfileConstant.ERROR, message);
        redirectAttributes.addFlashAttribute(UserProfileConstant.FIRST_NAME, firstName);
        redirectAttributes.addFlashAttribute(UserProfileConstant.LAST_NAME, lastName);
        return "redirect:" + UserProfileConstant.COMPLETE_PROFILE_PATH;
    }
}
