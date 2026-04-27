package com.njhtr.marltsc.fusion.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateVectorBO {
    private String intersectionId;
    private double[] values;
    private Long timestamp;
}
