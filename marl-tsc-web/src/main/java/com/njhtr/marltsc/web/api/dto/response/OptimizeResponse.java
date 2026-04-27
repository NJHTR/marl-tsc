package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;

@Data
public class OptimizeResponse {
    private Boolean success;
    private String resultId;
    private Integer estimatedTime;
    private String message;
}
