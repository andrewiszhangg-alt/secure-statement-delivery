package com.chitsa.account.validator;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.common.validator.OnCreate;
import com.chitsa.common.validator.OnUpdate;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.common.validator.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountValidator implements Validator<AccountRequestDto> {

    private final RequestBeanValidator beanValidator;

    @Override
    public void validate(AccountRequestDto target) {
        beanValidator.requireValid(target, AccountConstant.REQUEST_NOT_NULL, OnUpdate.class);
    }

    public void validateForCreate(AccountRequestDto target) {
        beanValidator.requireValid(target, AccountConstant.REQUEST_NOT_NULL, OnCreate.class);
    }
}
