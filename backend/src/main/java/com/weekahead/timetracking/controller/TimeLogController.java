package com.weekahead.timetracking.controller;

import com.weekahead.timetracking.dto.CreateTimeLogRequest;
import com.weekahead.timetracking.dto.TimeLogResponse;
import com.weekahead.timetracking.dto.UpdateTimeLogRequest;
import com.weekahead.timetracking.service.TimeLogService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-logs")
@SecurityRequirement(name = "bearerAuth")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(TimeLogService timeLogService) {
        this.timeLogService = timeLogService;
    }

    @PostMapping
    public ResponseEntity<TimeLogResponse> create(
            @Valid @RequestBody CreateTimeLogRequest request
    ) {
        return ResponseEntity.ok(timeLogService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeLogResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimeLogRequest request
    ) {
        return ResponseEntity.ok(timeLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeLogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TimeLogResponse>> findAll(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long lifeAreaId
    ) {
        return ResponseEntity.ok(
                timeLogService.findAll(from, to, lifeAreaId)
        );
    }
}