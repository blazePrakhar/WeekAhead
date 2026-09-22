package com.weekahead.neglect.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekahead.neglect.model.NeglectAssessment;
import com.weekahead.neglect.service.NeglectService;

@RestController
@RequestMapping("/api/neglect")
public class NeglectController {

    private final NeglectService neglectService;

    public NeglectController(NeglectService neglectService) {
        this.neglectService = neglectService;
    }

    @GetMapping
    public List<NeglectAssessment> getNeglectAssessments() {
        return neglectService.calculate();
    }
}