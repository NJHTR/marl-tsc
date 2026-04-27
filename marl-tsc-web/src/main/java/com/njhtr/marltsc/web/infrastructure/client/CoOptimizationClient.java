package com.njhtr.marltsc.web.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.infrastructure.client.dto.OptimizationResultResponse;
import com.njhtr.marltsc.web.infrastructure.client.dto.OptimizationTriggerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "co-optimization-service", url = "http://localhost:8083")
public interface CoOptimizationClient {

    @PostMapping("/api/v1/coopt/optimize")
    ApiResult<OptimizationResultResponse> triggerOptimize(@RequestBody OptimizationTriggerRequest request);

    @GetMapping("/api/v1/coopt/results/{intersectionId}")
    ApiResult<OptimizationResultResponse> getResult(@PathVariable("intersectionId") String intersectionId);
}
