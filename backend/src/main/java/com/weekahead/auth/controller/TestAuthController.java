package com.weekahead.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestAuthController {

    @GetMapping("/protected")
    public Map<String, String> protectedEndpoint() {
        return Map.of("message", "You are authenticated");
    }
}