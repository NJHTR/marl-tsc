package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class RouteRequest {
    private String originId;
    private String destinationId;
    private String vehicleType;
    private Integer priority;
}
