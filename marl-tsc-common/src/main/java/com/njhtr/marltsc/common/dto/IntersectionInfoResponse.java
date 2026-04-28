package com.njhtr.marltsc.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionInfoResponse {
    private String intersectionId;
    private String name;
    private double latitude;
    private double longitude;
    private int capacity;
    private List<RoadApproach> approaches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadApproach {
        private String direction;
        private int lanes;
        private double roadLength;
        private double roadWidth;
    }
}
