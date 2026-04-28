package com.njhtr.marltsc.common.dto;

import lombok.Data;

@Data
public class DrlActionResponse {
    private String intersectionId;
    private int action;
    private double[] qValues;
    private double confidence;
}
