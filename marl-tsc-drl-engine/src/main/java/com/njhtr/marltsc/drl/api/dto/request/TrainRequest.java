package com.njhtr.marltsc.drl.api.dto.request;

import lombok.Data;

@Data
public class TrainRequest {
    private String intersectionId;
    private double[] state;
    private int action;
    private double reward;
    private double[] nextState;
    private boolean done;
}
