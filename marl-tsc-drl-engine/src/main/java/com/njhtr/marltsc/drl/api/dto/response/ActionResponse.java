package com.njhtr.marltsc.drl.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActionResponse {
    private String intersectionId;
    private int action;
    private double[] qValues;
    private double confidence;
}
