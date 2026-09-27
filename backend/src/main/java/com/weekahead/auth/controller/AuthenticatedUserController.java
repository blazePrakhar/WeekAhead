package com.weekahead.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.auth.service.CurrentUserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
public class AuthenticatedUserController {

    private final CurrentUserService currentUserService;

    public AuthenticatedUserController(
            CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        var user = currentUserService.getCurrentUser();

        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        );
    }
}