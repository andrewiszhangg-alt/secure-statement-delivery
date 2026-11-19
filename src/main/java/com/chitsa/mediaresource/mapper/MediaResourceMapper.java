package com.chitsa.mediaresource.mapper;

import com.chitsa.common.mapper.DtoMapper;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.dto.MediaResourceResponseDto;
import com.chitsa.mediaresource.entity.MediaResource;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import org.springframework.stereotype.Component;

@Component
public class MediaResourceMapper
        implements DtoMapper<MediaResource, MediaResourceRequestDto, MediaResourceResponseDto> {

    @Override
    public MediaResource convertDtoToEntity(MediaResourceRequestDto dto) {
        var entity = new MediaResource();
        entity.setContext(dto.context());
        entity.setObjectKey(dto.objectKey());
        var file = dto.multipartFile();
        if (file != null) {
            entity.setContentType(file.getContentType());
            entity.setOriginalFilename(file.getOriginalFilename());
        }
        return entity;
    }

    @Override
    public MediaResourceResponseDto convertEntityToResponseDto(MediaResource entity) {
        return MediaResourceResponseDto.builder()
                .id(entity.getId())
                .context(entity.getContext())
                .objectKey(entity.getObjectKey())
                .url(entity.getUrl())
                .contentType(entity.getContentType())
                .originalFilename(entity.getOriginalFilename())
                .createTime(entity.getCreateTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }

    /** Media resources are immutable after create — update is not supported. */
    @Override
    public void updateEntityFromDto(MediaResourceRequestDto dto, MediaResource entity) {
        throw new UnsupportedOperationException(MediaResourceConstant.UPDATE_NOT_SUPPORTED);
    }
}
