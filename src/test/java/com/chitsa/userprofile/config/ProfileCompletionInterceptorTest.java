package com.chitsa.userprofile.config;

import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.util.UserProfileConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCompletionInterceptorTest {

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    @InjectMocks
    private ProfileCompletionInterceptor interceptor;

    @Test
    void completeProfileContinues() throws Exception {
        when(currentProfileProvider.profileComplete()).thenReturn(true);

        var allowed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void incompleteProfileIsSentToTheForm() throws Exception {
        when(currentProfileProvider.profileComplete()).thenReturn(false);
        var request = new MockHttpServletRequest();
        request.setContextPath("/statements");
        var response = new MockHttpServletResponse();

        var allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/statements" + UserProfileConstant.COMPLETE_PROFILE_PATH);
    }
}
