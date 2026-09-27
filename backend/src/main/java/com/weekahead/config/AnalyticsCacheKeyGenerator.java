package com.weekahead.config;

import java.lang.reflect.Method;

import com.weekahead.auth.service.CurrentUserService;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component("analyticsCacheKeyGenerator")
public class AnalyticsCacheKeyGenerator implements KeyGenerator {

    private final CurrentUserService currentUserService;

    public AnalyticsCacheKeyGenerator(
            CurrentUserService currentUserService
    ) {
        this.currentUserService = currentUserService;
    }

    @Override
    public Object generate(
            Object target,
            Method method,
            Object... params
    ) {
        Long userId = currentUserService.getCurrentUser().getId();

        int weeks = (Integer) params[0];

        return userId + ":" + weeks;
    }
}