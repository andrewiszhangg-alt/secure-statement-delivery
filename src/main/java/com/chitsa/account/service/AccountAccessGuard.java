package com.chitsa.account.service;

import com.chitsa.account.entity.Account;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Ownership checks: customers reach only their own accounts; admins reach any. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountAccessGuard {

    private final CurrentUser currentUser;
    private final CurrentProfileProvider currentProfileProvider;

    /** A foreign account is reported as not found so its existence is not revealed. */
    public void assertCanAccess(Account account) {
        if (currentUser.isAdmin() || account.getUserProfileId().equals(currentProfileProvider.profileId())) {
            return;
        }
        log.warn("Blocked access to accountId={} by idpUuid={}", account.getId(), currentUser.idpUuid());
        throw new AccountNotFoundException(AccountConstant.NOT_FOUND + account.getId());
    }

    public void assertAdmin() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException(AccountConstant.PROFILE_ACCESS_DENIED);
        }
    }

    public void assertCanAccessProfile(UUID userProfileId) {
        if (currentUser.isAdmin() || userProfileId.equals(currentProfileProvider.profileId())) {
            return;
        }
        log.warn("Blocked access to profileId={} by idpUuid={}", userProfileId, currentUser.idpUuid());
        throw new AccessDeniedException(AccountConstant.PROFILE_ACCESS_DENIED);
    }
}
