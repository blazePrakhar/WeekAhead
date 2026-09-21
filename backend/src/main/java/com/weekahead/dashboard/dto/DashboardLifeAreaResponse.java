package com.weekahead.dashboard.dto;

public record DashboardLifeAreaResponse(
        Long lifeAreaId,
        String lifeAreaName,
        Integer recommendedMinutes,
        Integer actualMinutes,
        Integer deficitMinutes,
        Integer overflowMinutes,
        boolean neglected
) {
}