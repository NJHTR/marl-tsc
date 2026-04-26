package com.njhtr.marltsc.route.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.route.api.dto.request.RouteRequest;
import com.njhtr.marltsc.route.api.dto.response.RouteResponse;
import com.njhtr.marltsc.route.service.api.RoutePlanningAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
public class RoutePlanningController {

    private final RoutePlanningAppService routePlanningAppService;

    @PostMapping("/plan")
    public ApiResult<RouteResponse> planRoute(@RequestBody RouteRequest request) {
        return ApiResult.ok(routePlanningAppService.computeRoute(request));
    }

    @PostMapping("/alternatives")
    public ApiResult<List<RouteResponse>> alternativeRoutes(@RequestBody RouteRequest request) {
        return ApiResult.ok(routePlanningAppService.computeAlternativeRoutes(request));
    }

    @GetMapping("/network/status")
    public ApiResult<Map<String, Double>> networkStatus() {
        return ApiResult.ok(routePlanningAppService.getNetworkStatus());
    }
}
