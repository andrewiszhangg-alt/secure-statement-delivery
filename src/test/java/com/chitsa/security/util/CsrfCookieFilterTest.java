package com.chitsa.security.util;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsrfCookieFilterTest {

    @Test
    void readsTheTokenSoTheCookieIsWritten() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        var token = mock(CsrfToken.class);
        when(token.getToken()).thenReturn("csrf");
        request.setAttribute(CsrfToken.class.getName(), token);

        new CsrfCookieFilter().doFilter(request, response, chain);

        verify(token).getToken();
        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesWhenNoTokenIsLoaded() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new CsrfCookieFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
