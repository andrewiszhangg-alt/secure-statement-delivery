package com.chitsa.accounttype.controller;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.accounttype.util.AccountTypeConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lets API clients look up the ids to send as {@code accountTypeId} when creating accounts. */
@RestController
@RequestMapping(AccountTypeConstant.API_PATH)
@RequiredArgsConstructor
public class AccountTypeRestController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    public List<AccountTypeResponseDto> findAll() {
        return accountTypeService.findAll();
    }
}
