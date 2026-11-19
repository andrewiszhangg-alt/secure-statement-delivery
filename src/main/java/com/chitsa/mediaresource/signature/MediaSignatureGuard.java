package com.chitsa.mediaresource.signature;

import com.chitsa.mediaresource.config.MediaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Refuses to start when a configured extension has no byte signature. */
@Component
@RequiredArgsConstructor
public class MediaSignatureGuard {

    private final MediaProperties properties;
    private final MediaSignatureRules rules;

    @PostConstruct
    void check() {
        rules.requireKnown(properties.getAllowedExtensions());
    }
}
