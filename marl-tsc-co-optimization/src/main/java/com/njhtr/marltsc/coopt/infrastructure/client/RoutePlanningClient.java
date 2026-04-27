package com.njhtr.marltsc.coopt.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.RouteRequest;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.RouteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "route-planning-service", url = "http://localhost:8082")
public interface RoutePlanningClient {
    
    @PostMapping("/api/v1/route/plan")
    ApiResult<RouteResponse> computeRoute(@RequestBody RouteRequest request);
    
    @GetMapping("/api/v1/route/network/status")
    ApiResult<Map<String, Double>> getNetworkStatus();
}
