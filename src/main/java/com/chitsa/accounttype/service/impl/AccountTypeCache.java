package com.chitsa.accounttype.service.impl;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/** The account-type catalog. A create drops it; otherwise it can be up to ten minutes old. */
@Component
public class AccountTypeCache {

    private static final String KEY = "all";

    private final Cache<String, List<AccountTypeResponseDto>> cache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public List<AccountTypeResponseDto> get(Supplier<List<AccountTypeResponseDto>> loader) {
        return cache.get(KEY, ignored -> List.copyOf(loader.get()));
    }

    public void invalidate() {
        cache.invalidateAll();
    }
}
