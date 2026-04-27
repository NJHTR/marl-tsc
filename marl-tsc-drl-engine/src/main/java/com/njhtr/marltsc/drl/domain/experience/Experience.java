package com.njhtr.marltsc.drl.domain.experience;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Experience {
    private double[] state;
    private int action;
    private double reward;
    private double[] nextState;
    private boolean done;
}
