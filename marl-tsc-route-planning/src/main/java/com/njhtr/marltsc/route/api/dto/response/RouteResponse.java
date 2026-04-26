package com.njhtr.marltsc.route.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RouteResponse {
    private String routeId;
    private String originId;
    private String destinationId;
    private Double totalDistance;
    private Double estimatedTime;
    private List<PathNodeResponse> path;
}
