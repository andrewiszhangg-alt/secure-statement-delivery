package com.chitsa.mediaresource.validator;

import com.chitsa.common.util.ValidationUtil;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.common.validator.Validator;
import com.chitsa.mediaresource.config.MediaProperties;
import com.chitsa.mediaresource.dto.MediaResourceRequestDto;
import com.chitsa.mediaresource.signature.MediaSignatureRules;
import com.chitsa.mediaresource.util.MediaResourceConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaResourceValidator implements Validator<MediaResourceRequestDto> {

    private final MediaProperties mediaProperties;
    private final RequestBeanValidator beanValidator;
    private final MediaSignatureRules signatureRules;

    @Override
    public void validate(MediaResourceRequestDto target) {
        beanValidator.requireValid(target, MediaResourceConstant.REQUEST_NOT_NULL);

        var file = target.multipartFile();
        ValidationUtil.require(!file.isEmpty(), MediaResourceConstant.FILE_EMPTY);

        validateContentType(file);
        var extension = validateExtension(file.getOriginalFilename());
        validateSignature(file, extension);
    }

    private void validateContentType(MultipartFile file) {
        var contentType = file.getContentType();
        var allowed = mediaProperties.getAllowedContentTypes();
        if (ValidationUtil.isBlank(contentType)
                || allowed.stream().noneMatch(contentType::equalsIgnoreCase)) {
            log.warn("Rejected contentType={}", contentType);
            ValidationUtil.fail(MediaResourceConstant.CONTENT_TYPE_NOT_ALLOWED.formatted(contentType, allowed));
        }
    }

    private void validateSignature(MultipartFile file, String extension) {
        byte[] header;
        try (var input = file.getInputStream()) {
            header = input.readNBytes(MediaSignatureRules.PDF.length);
        } catch (IOException ex) {
            rejectSignature("Could not read upload signature: {}", ex.getMessage());
            return;
        }
        if (!signatureRules.matches(extension, header)) {
            rejectSignature("Rejected signature for extension={}", extension);
        }
    }

    private String validateExtension(String originalFilename) {
        ValidationUtil.requireNotBlank(originalFilename, MediaResourceConstant.FILENAME_BLANK);
        var extension = extensionOf(originalFilename);
        var allowed = mediaProperties.getAllowedExtensions();
        if (allowed.stream().noneMatch(extension::equalsIgnoreCase)) {
            log.warn("Rejected extension={} for file={}", extension, originalFilename);
            ValidationUtil.fail(MediaResourceConstant.EXTENSION_NOT_ALLOWED.formatted(originalFilename, allowed));
        }
        return extension;
    }

    private static void rejectSignature(String detail, Object argument) {
        log.warn(detail, argument);
        ValidationUtil.fail(MediaResourceConstant.SIGNATURE_MISMATCH);
    }

    private String extensionOf(String filename) {
        var lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            ValidationUtil.fail(MediaResourceConstant.NO_EXTENSION);
        }
        return filename.substring(lastDot + 1);
    }
}
