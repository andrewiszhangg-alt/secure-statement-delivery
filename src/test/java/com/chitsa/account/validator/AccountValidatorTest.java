package com.chitsa.account.validator;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountValidatorTest {

    private final AccountValidator validator = new AccountValidator(
            new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void createRequiresTheOwnerAndType() {
        assertThatThrownBy(() -> validator.validateForCreate(new AccountRequestDto(null, null)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("userProfileId")
                .hasMessageContaining("accountTypeId");
    }

    @Test
    void updateRequiresTheType() {
        assertThatThrownBy(() -> validator.validate(new AccountRequestDto(null, null)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("accountTypeId");
    }

    @Test
    void rejectsANullRequest() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(AccountConstant.REQUEST_NOT_NULL);
    }
}
