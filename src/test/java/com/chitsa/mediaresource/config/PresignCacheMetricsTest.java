package com.chitsa.mediaresource.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PresignCacheMetricsTest {

    @Test
    void publishesGetsForThePresignCache() {
        var cache = Caffeine.newBuilder().recordStats().<String, String>build();
        var registry = new SimpleMeterRegistry();
        new PresignCacheMetrics(cache, registry);
        cache.put("media", "https://signed.example");
        cache.getIfPresent("media");

        assertThat(registry.get("cache.gets").tag("result", "hit").functionCounter().count()).isEqualTo(1);
    }
}
