package com.chitsa.mediaresource.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.stereotype.Component;

/** Publishes presign cache hits, misses, and evictions. */
@Component
public class PresignCacheMetrics {

    public PresignCacheMetrics(Cache<String, String> presignedUrlCache, MeterRegistry meterRegistry) {
        CaffeineCacheMetrics.monitor(meterRegistry, presignedUrlCache, "presignedUrls");
    }
}
