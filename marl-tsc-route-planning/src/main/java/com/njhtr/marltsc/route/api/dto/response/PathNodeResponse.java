package com.njhtr.marltsc.route.api.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PathNodeResponse {
    private String intersectionId;
    private String intersectionName;
    private Double longitude;
    private Double latitude;
    private LocalDateTime expectedArrivalTime;
}
