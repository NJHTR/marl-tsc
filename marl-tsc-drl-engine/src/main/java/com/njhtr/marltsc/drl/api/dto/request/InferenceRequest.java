package com.njhtr.marltsc.drl.api.dto.request;

import lombok.Data;

@Data
public class InferenceRequest {
    private String intersectionId;
    private double[] state;
}
