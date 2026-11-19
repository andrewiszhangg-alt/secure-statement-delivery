package com.chitsa.userprofile.controller;

import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(UserProfileConstant.API_PATH)
@RequiredArgsConstructor
public class UserProfileRestController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<UserProfileResponseDto> create(@RequestBody UserProfileRequestDto requestDto) {
        log.info("POST /api/profiles");
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.create(requestDto));
    }

    @PutMapping("/{id}")
    public UserProfileResponseDto update(@PathVariable UUID id, @RequestBody UserProfileRequestDto requestDto) {
        return userProfileService.update(id, requestDto);
    }

    @GetMapping("/{id}")
    public UserProfileResponseDto findById(@PathVariable UUID id) {
        return userProfileService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        userProfileService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
