package com.chitsa.userprofile.service.impl;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedProfileProviderTest {

    @Mock
    private CurrentUser currentUser;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private AuthenticatedProfileProvider provider;

    @Test
    void returnsTheProfileForTheSignedInUser() {
        var idpUuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var profileId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(currentUser.email()).thenReturn("ada@example.com");
        when(currentUser.idpUuid()).thenReturn(idpUuid);
        when(currentUser.firstName()).thenReturn("Ada");
        when(currentUser.lastName()).thenReturn("Lovelace");
        when(userProfileService.findOrCreateByIdpUuid(any())).thenReturn(
                UserProfileResponseDto.builder().id(profileId).email("ada@example.com").profileComplete(true).build());

        assertThat(provider.profileId()).isEqualTo(profileId);
    }

    @Test
    void resolvesTheProfileOncePerRequest() {
        var profileId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(currentUser.email()).thenReturn("ada@example.com");
        when(currentUser.idpUuid()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(userProfileService.findOrCreateByIdpUuid(any())).thenReturn(
                UserProfileResponseDto.builder().id(profileId).profileComplete(true).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            assertThat(provider.profileId()).isEqualTo(profileId);
            assertThat(provider.profileComplete()).isTrue();
            verify(userProfileService, times(1)).findOrCreateByIdpUuid(any());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void rejectsASignInWithoutAnEmail() {
        when(currentUser.email()).thenReturn(" ");

        assertThatThrownBy(provider::profileId)
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(UserProfileConstant.MISSING_EMAIL);
    }
}
