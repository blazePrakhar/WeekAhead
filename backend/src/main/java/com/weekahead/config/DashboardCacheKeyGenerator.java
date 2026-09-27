package com.weekahead.config;

import java.lang.reflect.Method;
import java.time.LocalDate;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import com.weekahead.auth.service.CurrentUserService;

@Component("dashboardCacheKeyGenerator")
public class DashboardCacheKeyGenerator implements KeyGenerator {

    private final CurrentUserService currentUserService;

    public DashboardCacheKeyGenerator(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public Object generate(
            Object target,
            Method method,
            Object... params
    ) {
        Long userId = currentUserService.getCurrentUser().getId();

        return userId + ":" + LocalDate.now();
    }
}