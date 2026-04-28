package com.njhtr.marltsc.signal.infrastructure.client.dto;

import lombok.Data;

@Data
public class DrlActionResponse {
    private String intersectionId;
    private int action;
    private double[] qValues;
    private double confidence;
}
