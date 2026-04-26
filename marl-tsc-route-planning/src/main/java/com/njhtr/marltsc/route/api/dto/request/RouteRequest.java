package com.njhtr.marltsc.route.api.dto.request;

import lombok.Data;

@Data
public class RouteRequest {
    private String originId;
    private String destinationId;
    private String vehicleType;
    private Integer priority;
}
