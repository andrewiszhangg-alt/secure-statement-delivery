package com.chitsa.mediaresource.signature;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Byte signatures for configured upload extensions. PDF is the only rule shipped today. */
@Component
public class MediaSignatureRules {

    public static final byte[] PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);

    public void requireKnown(List<String> extensions) {
        for (var extension : extensions) {
            if (!supports(extension)) {
                throw new IllegalStateException("No byte signature registered for extension: " + extension);
            }
        }
    }

    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    public boolean matches(String extension, byte[] header) {
        if (!supports(extension) || header == null) {
            return false;
        }
        return startsWith(header, PDF);
    }

    public static String normalize(String extension) {
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] header, byte[] prefix) {
        if (header.length < prefix.length) {
            return false;
        }
        for (var i = 0; i < prefix.length; i++) {
            if (header[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
