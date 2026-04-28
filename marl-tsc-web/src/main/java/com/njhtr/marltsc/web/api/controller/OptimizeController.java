package com.njhtr.marltsc.web.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.api.dto.request.OptimizeRequest;
import com.njhtr.marltsc.web.api.dto.response.OptimizeResponse;
import com.njhtr.marltsc.web.infrastructure.client.CoOptimizationClient;
import com.njhtr.marltsc.web.infrastructure.client.dto.OptimizationResultResponse;
import com.njhtr.marltsc.web.service.api.DashboardAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/web/optimize")
@RequiredArgsConstructor
public class OptimizeController {

    private final DashboardAppService dashboardAppService;
    private final CoOptimizationClient coOptimizationClient;

    @PostMapping
    public ApiResult<OptimizeResponse> optimize(@RequestBody OptimizeRequest request) {
        OptimizeResponse response = dashboardAppService.triggerOptimization(
                request.getIntersectionId(), request.getStrategy());
        return ApiResult.ok(response);
    }

    @GetMapping("/status/{intersectionId}")
    public ApiResult<OptimizeResponse> getOptimizeStatus(@PathVariable String intersectionId) {
        OptimizeResponse response = new OptimizeResponse();
        try {
            ApiResult<OptimizationResultResponse> result = coOptimizationClient.getResult(intersectionId);
            if (result != null && result.getData() != null) {
                response.setSuccess(true);
                response.setResultId(result.getData().getLogId());
                response.setEstimatedTime(result.getData().getConvergenceIterations());
                response.setMessage("优化状态: reward=" + result.getData().getJointReward());
            } else {
                response.setSuccess(false);
                response.setMessage("暂无优化结果");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch optimization status for {}: {}", intersectionId, e.getMessage());
            response.setSuccess(false);
            response.setMessage("查询优化状态失败: " + e.getMessage());
        }
        return ApiResult.ok(response);
    }
}
