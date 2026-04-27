package com.njhtr.marltsc.route.service.api;

import com.njhtr.marltsc.route.api.dto.request.RouteRequest;
import com.njhtr.marltsc.route.api.dto.response.RouteResponse;

import java.util.List;
import java.util.Map;

public interface RoutePlanningAppService {

    RouteResponse computeRoute(RouteRequest request);

    List<RouteResponse> computeAlternativeRoutes(RouteRequest request);

    Map<String, Double> getNetworkStatus();
}
