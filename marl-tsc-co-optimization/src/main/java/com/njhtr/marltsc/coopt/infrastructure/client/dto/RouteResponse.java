package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteResponse {
    private String vehicleId;
    private List<String> path;
    private Double estimatedTime;
}
