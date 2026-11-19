package com.chitsa.mediaresource.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    private List<String> allowedContentTypes = new ArrayList<>();
    private List<String> allowedExtensions = new ArrayList<>();

    /** Browser file-input hint, for example {@code .pdf}. */
    public String acceptAttribute() {
        return allowedExtensions.stream()
                .filter(extension -> extension != null && !extension.isBlank())
                .map(extension -> "." + extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
    }
}
