package com.chitsa.accountstatement.validator;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.common.validator.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountStatementValidator implements Validator<AccountStatementRequestDto> {

    private final RequestBeanValidator beanValidator;

    @Override
    public void validate(AccountStatementRequestDto target) {
        beanValidator.requireValid(target, AccountStatementConstant.REQUEST_NOT_NULL);
    }
}
