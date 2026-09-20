package com.weekahead.lifearea.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.lifearea.dto.LifeAreaRequest;
import com.weekahead.lifearea.dto.LifeAreaResponse;
import com.weekahead.lifearea.service.LifeAreaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/life-areas")
public class LifeAreaController {

    private final LifeAreaService lifeAreaService;

    public LifeAreaController(LifeAreaService lifeAreaService) {
        this.lifeAreaService = lifeAreaService;
    }

    @PostMapping
    public ResponseEntity<LifeAreaResponse> create(
            @Valid @RequestBody LifeAreaRequest request
    ) {
        return ResponseEntity.ok(lifeAreaService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LifeAreaResponse>> getAll() {
        return ResponseEntity.ok(lifeAreaService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LifeAreaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LifeAreaRequest request
    ) {
        return ResponseEntity.ok(lifeAreaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lifeAreaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}