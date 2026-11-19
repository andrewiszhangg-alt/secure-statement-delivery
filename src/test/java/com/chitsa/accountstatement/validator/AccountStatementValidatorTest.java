package com.chitsa.accountstatement.validator;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountStatementValidatorTest {

    private final AccountStatementValidator validator = new AccountStatementValidator(
            new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void createRequiresTheAccountAndFile() {
        assertThatThrownBy(() -> validator.validate(new AccountStatementRequestDto(null, null)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("accountId")
                .hasMessageContaining("multipartFile");
    }

    @Test
    void rejectsANullRequest() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage(AccountStatementConstant.REQUEST_NOT_NULL);
    }
}
