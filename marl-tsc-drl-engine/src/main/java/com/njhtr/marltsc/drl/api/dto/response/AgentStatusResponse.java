package com.njhtr.marltsc.drl.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentStatusResponse {
    private String intersectionId;
    private boolean trainingMode;
    private double epsilon;
    private int replayBufferSize;
    private int totalSteps;
}
