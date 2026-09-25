package com.weekahead.config;

import java.time.LocalDate;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class DashboardCacheInvalidationService {

    private final CacheManager cacheManager;

    public DashboardCacheInvalidationService(
            CacheManager cacheManager
    ) {
        this.cacheManager = cacheManager;
    }

    public void invalidate(Long userId) {
        Cache cache = cacheManager.getCache("dashboard");

        if (cache != null) {
            cache.evict(userId + ":" + LocalDate.now());
        }
    }
}