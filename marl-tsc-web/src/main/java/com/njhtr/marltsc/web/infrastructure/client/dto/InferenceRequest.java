package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class InferenceRequest {
    private String intersectionId;
    private double[] state;
}
