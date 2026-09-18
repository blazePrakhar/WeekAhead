package com.weekahead.auth.dto;

public record LoginResponse(
        String message,
        String accessToken
) {
}