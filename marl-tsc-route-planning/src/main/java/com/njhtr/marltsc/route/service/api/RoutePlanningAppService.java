package com.njhtr.marltsc.route.service.api;

import com.njhtr.marltsc.route.api.dto.request.RouteRequest;
import com.njhtr.marltsc.route.api.dto.response.RouteResponse;

import java.util.List;

public interface RoutePlanningAppService {

    RouteResponse computeRoute(RouteRequest request);

    List<RouteResponse> computeAlternativeRoutes(RouteRequest request);
}
