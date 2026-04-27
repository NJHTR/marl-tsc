package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {
    private String intersectionId;
    private int action;
    private double[] qValues;
    private double confidence;
}
