package com.njhtr.marltsc.fusion.api.dto.response;

import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;
import lombok.Data;

import java.util.Map;

@Data
public class FusionResultResponse {
    private String dataId;
    private DataSourceType sourceType;
    private String intersectionId;
    private Long timestamp;
    private Double qualityScore;
    private Map<String, Object> fusedFeatures;
}
