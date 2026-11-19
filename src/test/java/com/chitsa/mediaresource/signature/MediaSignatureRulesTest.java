package com.chitsa.mediaresource.signature;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaSignatureRulesTest {

    private final MediaSignatureRules rules = new MediaSignatureRules();

    @Test
    void pdfHeaderMatches() {
        assertThat(rules.matches("pdf", "%PDF-1.4".getBytes())).isTrue();
        assertThat(rules.matches("PDF", "not-a-pdf".getBytes())).isFalse();
    }

    @Test
    void anUnknownExtensionFailsStartup() {
        assertThatThrownBy(() -> rules.requireKnown(List.of("pdf", "txt")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No byte signature registered for extension: txt");
    }

    @Test
    void theShippedPdfExtensionIsKnown() {
        rules.requireKnown(List.of("pdf"));
        assertThat(rules.supports("pdf")).isTrue();
    }
}
