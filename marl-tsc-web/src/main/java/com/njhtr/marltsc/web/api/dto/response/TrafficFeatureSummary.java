package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;

@Data
public class TrafficFeatureSummary {
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
