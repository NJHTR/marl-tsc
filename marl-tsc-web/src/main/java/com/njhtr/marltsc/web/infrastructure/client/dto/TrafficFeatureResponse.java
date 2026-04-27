package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class TrafficFeatureResponse {
    private String intersectionId;
    private Long timestamp;
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
