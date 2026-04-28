package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrlInferenceRequest {
    private String intersectionId;
    private double[] state;
}
