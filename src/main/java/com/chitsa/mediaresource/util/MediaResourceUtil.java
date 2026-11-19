package com.chitsa.mediaresource.util;

import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

@UtilityClass
public class MediaResourceUtil {

    /** Builds a create request from the storage context, object key and file. */
    public static MediaResourceRequestDto toCreateRequest(String context, String objectKey, MultipartFile file) {
        return new MediaResourceRequestDto(context, objectKey, file);
    }
}
