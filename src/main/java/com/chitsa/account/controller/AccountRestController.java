package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AccountConstant.API_PATH)
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDto> create(@RequestBody AccountRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(requestDto));
    }

    @PutMapping("/{id}")
    public AccountResponseDto update(@PathVariable UUID id, @RequestBody AccountRequestDto requestDto) {
        return accountService.update(id, requestDto);
    }

    @GetMapping("/{id}")
    public AccountResponseDto findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @GetMapping
    public List<AccountResponseDto> findByUserProfileId(@RequestParam UUID userProfileId) {
        return accountService.findByUserProfileId(userProfileId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        accountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
