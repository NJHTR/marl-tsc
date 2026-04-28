package com.njhtr.marltsc.fusion.api.dto.response;

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
    /** Approach roads: N, S, E, W */
    private List<RoadApproach> approaches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadApproach {
        private String direction;     // N, S, E, W
        private int lanes;
        private double roadLength;    // meters (visual length in 3D scene)
        private double roadWidth;     // meters per lane
    }
}
