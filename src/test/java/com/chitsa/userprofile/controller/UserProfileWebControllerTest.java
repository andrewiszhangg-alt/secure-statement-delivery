package com.chitsa.userprofile.controller;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.PersistenceConstant;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileWebControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    @InjectMocks
    private UserProfileWebController controller;

    @Test
    void formSendsACompleteProfileToAccounts() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        when(userProfileService.isProfileComplete(profileId)).thenReturn(true);

        assertThat(controller.form(new ExtendedModelMap())).isEqualTo(UserProfileConstant.ACCOUNTS_REDIRECT);
    }

    @Test
    void formShowsTheStoredNames() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        when(userProfileService.isProfileComplete(profileId)).thenReturn(false);
        when(userProfileService.findById(profileId)).thenReturn(profile("Ada", "Lovelace"));
        var model = new ExtendedModelMap();

        assertThat(controller.form(model)).isEqualTo(UserProfileConstant.COMPLETE_PROFILE_VIEW);
        assertThat(model.getAttribute(UserProfileConstant.EMAIL)).isEqualTo("ada@example.com");
        assertThat(model.getAttribute(UserProfileConstant.FIRST_NAME)).isEqualTo("Ada");
        assertThat(model.getAttribute(UserProfileConstant.LAST_NAME)).isEqualTo("Lovelace");
    }

    @Test
    void formKeepsNamesFromAFailedSubmit() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        when(userProfileService.isProfileComplete(profileId)).thenReturn(false);
        when(userProfileService.findById(profileId)).thenReturn(profile("Ada", "Lovelace"));
        var model = new ExtendedModelMap();
        model.addAttribute(UserProfileConstant.FIRST_NAME, "Grace");
        model.addAttribute(UserProfileConstant.LAST_NAME, "Hopper");

        controller.form(model);

        assertThat(model.getAttribute(UserProfileConstant.FIRST_NAME)).isEqualTo("Grace");
        assertThat(model.getAttribute(UserProfileConstant.LAST_NAME)).isEqualTo("Hopper");
    }

    @Test
    void saveRedirectsAfterTheProfileIsStored() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.save("Grace", "Hopper", redirect)).isEqualTo(UserProfileConstant.ACCOUNTS_REDIRECT);
        assertThat(redirect.getFlashAttributes().get(UserProfileConstant.MESSAGE)).isEqualTo(UserProfileConstant.PROFILE_SAVED);
    }

    @Test
    void saveReturnsToTheFormWhenTheNamesAreRejected() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        doThrow(new InvalidParameterException("firstName must not be blank"))
                .when(userProfileService).update(any(), any());
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.save(" ", "Hopper", redirect))
                .isEqualTo("redirect:" + UserProfileConstant.COMPLETE_PROFILE_PATH);
        assertThat(redirect.getFlashAttributes().get(UserProfileConstant.ERROR)).isEqualTo(UserProfileConstant.NAMES_REQUIRED);
    }

    @Test
    void saveReturnsToTheFormWhenTheVersionIsStale() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        doThrow(new ObjectOptimisticLockingFailureException("UserProfile", profileId))
                .when(userProfileService).update(any(), any());
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.save("Grace", "Hopper", redirect))
                .isEqualTo("redirect:" + UserProfileConstant.COMPLETE_PROFILE_PATH);
        assertThat(redirect.getFlashAttributes().get(UserProfileConstant.ERROR)).isEqualTo(PersistenceConstant.VERSION_CONFLICT);
        assertThat(redirect.getFlashAttributes().get(UserProfileConstant.FIRST_NAME)).isEqualTo("Grace");
    }

    private static UserProfileResponseDto profile(String firstName, String lastName) {
        return UserProfileResponseDto.builder()
                .id(UUID.randomUUID())
                .email("ada@example.com")
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
