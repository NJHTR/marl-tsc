package com.njhtr.marltsc.web.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.infrastructure.client.dto.PhaseAdjustRequest;
import com.njhtr.marltsc.web.infrastructure.client.dto.SignalPlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "signal-control-service", url = "http://localhost:8081")
public interface SignalControlClient {

    @GetMapping("/api/v1/signal/plans/{intersectionId}")
    ApiResult<SignalPlanResponse> getPlan(@PathVariable("intersectionId") String intersectionId);

    @PostMapping("/api/v1/signal/plans/adjust")
    ApiResult<Void> adjustPhase(@RequestBody PhaseAdjustRequest request);
}
