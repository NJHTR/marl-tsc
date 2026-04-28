package com.njhtr.marltsc.coopt.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.PhaseAdjustRequest;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.SignalPlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "signal-control-service", url = "http://localhost:8081")
public interface SignalControlClient {
    
    @GetMapping("/api/v1/signal/plans/{intersectionId}")
    ApiResult<SignalPlanResponse> getCurrentPlan(@PathVariable("intersectionId") String intersectionId);
    
    @PostMapping("/api/v1/signal/plans/adjust")
    ApiResult<Void> adjustPhase(@RequestBody PhaseAdjustRequest request);
}
