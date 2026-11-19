package com.chitsa.userprofile.mapper;

import com.chitsa.common.mapper.DtoMapper;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper implements DtoMapper<UserProfile, UserProfileRequestDto, UserProfileResponseDto> {

    @Override
    public UserProfile convertDtoToEntity(UserProfileRequestDto dto) {
        var entity = new UserProfile();
        entity.setEmail(dto.email().trim());
        entity.setFirstName(trimToNull(dto.firstName()));
        entity.setLastName(trimToNull(dto.lastName()));
        return entity;
    }

    @Override
    public UserProfileResponseDto convertEntityToResponseDto(UserProfile entity) {
        return UserProfileResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .createTime(entity.getCreateTime())
                .updatedTime(entity.getUpdatedTime())
                .profileComplete(entity.isProfileComplete())
                .build();
    }

    /** Copies the first and last name onto the profile. */
    @Override
    public void updateEntityFromDto(UserProfileRequestDto dto, UserProfile entity) {
        entity.setFirstName(dto.firstName().trim());
        entity.setLastName(dto.lastName().trim());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
