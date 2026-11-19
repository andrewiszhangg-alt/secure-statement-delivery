package com.chitsa.mediaresource.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Presigned GET URL cache. TTL is signature duration minus {@code presign-cache-buffer-minutes}. */
@Configuration
public class CacheConfig {

    private static final int PRESIGNED_URL_CACHE_SIZE = 10_000;

    @Bean
    Cache<String, String> presignedUrlCache(AwsS3Properties awsS3Properties) {
        return Caffeine.newBuilder()
                .maximumSize(PRESIGNED_URL_CACHE_SIZE)
                .expireAfterWrite(awsS3Properties.getS3().cacheTtl())
                .recordStats()
                .build();
    }

    /** Stops an in-flight sign from storing a URL after that media row was deleted. */
    @Bean
    Cache<String, Boolean> presignedUrlTombstones(AwsS3Properties awsS3Properties) {
        return Caffeine.newBuilder()
                .maximumSize(PRESIGNED_URL_CACHE_SIZE)
                .expireAfterWrite(awsS3Properties.getS3().cacheTtl())
                .build();
    }
}
