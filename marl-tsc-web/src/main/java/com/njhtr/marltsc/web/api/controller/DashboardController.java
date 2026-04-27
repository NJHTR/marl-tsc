package com.njhtr.marltsc.web.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.api.dto.response.DashboardResponse;
import com.njhtr.marltsc.web.api.dto.response.IntersectionSummaryResponse;
import com.njhtr.marltsc.web.service.api.DashboardAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardAppService dashboardAppService;

    @GetMapping("/dashboard/{intersectionId}")
    public ApiResult<DashboardResponse> getDashboard(@PathVariable String intersectionId) {
        DashboardResponse response = dashboardAppService.getDashboardData(intersectionId);
        return ApiResult.ok(response);
    }

    @GetMapping("/intersections")
    public ApiResult<List<IntersectionSummaryResponse>> getIntersections() {
        return ApiResult.ok(dashboardAppService.listIntersections());
    }
}
