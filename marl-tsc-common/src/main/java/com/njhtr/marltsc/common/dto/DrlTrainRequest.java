package com.njhtr.marltsc.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrlTrainRequest {
    private String intersectionId;
    private double[] state;
    private int action;
    private double reward;
    private double[] nextState;
    private boolean done;
}
