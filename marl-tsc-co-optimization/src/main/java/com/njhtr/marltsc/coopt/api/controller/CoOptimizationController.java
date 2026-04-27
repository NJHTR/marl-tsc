package com.njhtr.marltsc.coopt.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.api.dto.request.OptimizationTriggerRequest;
import com.njhtr.marltsc.coopt.api.dto.response.OptimizationResultResponse;
import com.njhtr.marltsc.coopt.service.api.CoOptimizationAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coopt")
@RequiredArgsConstructor
public class CoOptimizationController {

    private final CoOptimizationAppService coOptimizationAppService;

    @PostMapping("/optimize")
    public ApiResult<OptimizationResultResponse> optimize(@RequestBody OptimizationTriggerRequest request) {
        coOptimizationAppService.triggerOptimization(request.getIntersectionId());
        OptimizationResultResponse result = coOptimizationAppService.getLatestResult(request.getIntersectionId());
        return ApiResult.ok(result);
    }

    @GetMapping("/results/{intersectionId}")
    public ApiResult<OptimizationResultResponse> getResult(@PathVariable String intersectionId) {
        return ApiResult.ok(coOptimizationAppService.getLatestResult(intersectionId));
    }

    @GetMapping("/health")
    public ApiResult<String> health() {
        return ApiResult.ok("co-optimization-service alive");
    }
}
