package com.njhtr.marltsc.web.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.api.dto.request.OptimizeRequest;
import com.njhtr.marltsc.web.api.dto.response.OptimizeResponse;
import com.njhtr.marltsc.web.service.api.DashboardAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/web/optimize")
@RequiredArgsConstructor
public class OptimizeController {

    private final DashboardAppService dashboardAppService;

    @PostMapping
    public ApiResult<OptimizeResponse> optimize(@RequestBody OptimizeRequest request) {
        OptimizeResponse response = dashboardAppService.triggerOptimization(
                request.getIntersectionId(), request.getStrategy());
        return ApiResult.ok(response);
    }

    @GetMapping("/status/{resultId}")
    public ApiResult<OptimizeResponse> getOptimizeStatus(@PathVariable String resultId) {
        OptimizeResponse response = new OptimizeResponse();
        response.setResultId(resultId);
        response.setSuccess(true);
        response.setMessage("优化已完成");
        return ApiResult.ok(response);
    }
}
