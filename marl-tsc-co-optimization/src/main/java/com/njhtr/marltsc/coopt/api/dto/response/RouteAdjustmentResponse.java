package com.njhtr.marltsc.coopt.api.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class RouteAdjustmentResponse {
    private String vehicleId;
    private List<String> newPath;
    private Double estimatedTimeSaved;
}
