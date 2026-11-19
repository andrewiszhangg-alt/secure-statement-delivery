package com.chitsa.mediaresource.service;

import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.dto.MediaResourceResponseDto;

import java.util.UUID;

public interface MediaResourceService {

    MediaResourceResponseDto create(MediaResourceRequestDto requestDto);

    MediaResourceResponseDto findById(UUID id);

    void deleteById(UUID id);

    void deleteByObjectKey(String objectKey);

    String generatePreSignedUrlForResource(UUID id);

    String generatePreSignedUrlForResource(String context, String objectKey);
}
