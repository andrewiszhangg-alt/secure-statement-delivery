package com.chitsa.admin;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.security.service.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLinkAdviceTest {

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private AdminLinkAdvice advice;

    @Test
    void anAdminSeesTheLink() {
        when(currentUser.isAdmin()).thenReturn(true);

        assertThat(advice.isAdmin()).isTrue();
    }

    @Test
    void aCustomerDoesNotSeeTheLink() {
        when(currentUser.isAdmin()).thenReturn(false);

        assertThat(advice.isAdmin()).isFalse();
    }

    @Test
    void anAnonymousCallerDoesNotSeeTheLink() {
        when(currentUser.isAdmin()).thenThrow(new AccessDeniedException("nope"));

        assertThat(advice.isAdmin()).isFalse();
    }

    @Test
    void aCallerWithoutClaimsDoesNotSeeTheLink() {
        when(currentUser.isAdmin()).thenThrow(new InvalidParameterException("nope"));

        assertThat(advice.isAdmin()).isFalse();
    }
}
