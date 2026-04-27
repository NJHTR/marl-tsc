package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteRequest {
    private String origin;
    private String destination;
    private List<String> avoidSegments;
}
