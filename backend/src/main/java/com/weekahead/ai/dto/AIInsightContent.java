package com.weekahead.ai.dto;

import java.util.List;

public record AIInsightContent(
        String summary,
        List<String> observations,
        List<String> actions
) {
}