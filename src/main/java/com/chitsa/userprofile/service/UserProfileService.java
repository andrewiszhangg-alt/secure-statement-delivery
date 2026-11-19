package com.chitsa.userprofile.service;

import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.dto.UserProfileResponseDto;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponseDto create(UserProfileRequestDto requestDto);

    /** Updates first and last name. */
    UserProfileResponseDto update(UUID id, UserProfileRequestDto requestDto);

    UserProfileResponseDto findById(UUID id);

    UserProfileResponseDto findByIdpUuid(UUID idpUuid);

    /** Returns the profile for the idpUuid, creating it on first sign-in. */
    UserProfileResponseDto findOrCreateByIdpUuid(UserProfileRequestDto requestDto);

    /** Fails with a conflict when the profile still owns accounts. */
    void deleteById(UUID id);

    /** Throws UserProfileNotFoundException when the profile does not exist. */
    void requireExists(UUID id);

    /** True when the profile has a first name, last name, and email. */
    boolean isProfileComplete(UUID id);

    /** Throws when the profile is missing or has no first name, last name, or email. */
    void requireCompleteProfile(UUID id);
}
