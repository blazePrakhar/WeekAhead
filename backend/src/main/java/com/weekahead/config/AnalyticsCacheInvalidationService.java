package com.weekahead.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsCacheInvalidationService {

    private final CacheManager cacheManager;

    public AnalyticsCacheInvalidationService(
            CacheManager cacheManager
    ) {
        this.cacheManager = cacheManager;
    }

    public void invalidate() {
        Cache cache = cacheManager.getCache("analytics");

        if (cache != null) {
            cache.clear();
        }
    }
}