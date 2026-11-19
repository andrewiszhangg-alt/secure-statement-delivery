package com.chitsa.userprofile.validator;

import com.chitsa.common.validator.OnCreate;
import com.chitsa.common.validator.OnUpdate;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.common.validator.Validator;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.util.UserProfileConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileValidator implements Validator<UserProfileRequestDto> {

    private final RequestBeanValidator beanValidator;

    /** Update rules: both names are required. */
    @Override
    public void validate(UserProfileRequestDto target) {
        beanValidator.requireValid(target, UserProfileConstant.REQUEST_NOT_NULL, OnUpdate.class);
    }

    /** Create rules: idpUuid and a valid email are required. */
    public void validateForCreate(UserProfileRequestDto target) {
        beanValidator.requireValid(target, UserProfileConstant.REQUEST_NOT_NULL, OnCreate.class);
    }
}
