package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;

@Data
public class IntersectionSummaryResponse {
    private String intersectionId;
    private String name;
    private String status;
    private String congestionLevel;
    private Long lastUpdateTime;
}
