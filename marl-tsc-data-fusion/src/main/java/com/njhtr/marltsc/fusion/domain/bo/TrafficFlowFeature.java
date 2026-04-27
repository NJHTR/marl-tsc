package com.njhtr.marltsc.fusion.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficFlowFeature {
    private String intersectionId;
    private Long timestamp;
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
