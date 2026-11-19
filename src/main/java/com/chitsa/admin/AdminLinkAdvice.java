package com.chitsa.admin;

import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.security.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Puts {@code isAdmin} on every page so the footer link can stay hidden for customers. */
@ControllerAdvice
@RequiredArgsConstructor
public class AdminLinkAdvice {

    private final CurrentUser currentUser;

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        try {
            return currentUser.isAdmin();
        } catch (AccessDeniedException | InvalidParameterException ex) {
            return false;
        }
    }
}
