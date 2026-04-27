package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;

@Data
public class RouteSummary {
    private String routeId;
    private String originId;
    private String destinationId;
    private Double estimatedTime;
    private Double totalDistance;
}
