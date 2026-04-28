package com.njhtr.marltsc.route.infrastructure.client;

import com.njhtr.marltsc.common.dto.SignalPlanResponse;
import com.njhtr.marltsc.common.result.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "signal-control-service", url = "http://localhost:8081")
public interface SignalControlClient {

    @GetMapping("/api/v1/signal/plans/{intersectionId}")
    ApiResult<SignalPlanResponse> getPlan(@PathVariable("intersectionId") String intersectionId);
}
