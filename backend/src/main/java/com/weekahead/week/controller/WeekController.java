package com.weekahead.week.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.week.dto.WeekRequest;
import com.weekahead.week.dto.WeekResponse;
import com.weekahead.week.service.WeekService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/weeks")
public class WeekController {

    private final WeekService weekService;

    public WeekController(WeekService weekService) {
        this.weekService = weekService;
    }

    @PostMapping
    public ResponseEntity<WeekResponse> create(
            @Valid @RequestBody WeekRequest request
    ) {
        return ResponseEntity.ok(weekService.create(request));
    }

    @GetMapping("/current")
    public ResponseEntity<WeekResponse> getCurrent() {
        return ResponseEntity.ok(weekService.getCurrent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeekResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(weekService.getById(id));
    }
}