package com.njhtr.marltsc.fusion.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StateVectorResponse {
    private String intersectionId;
    private List<Double> values;
    private Long timestamp;
}
