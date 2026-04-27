package com.njhtr.marltsc.drl.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TrainingStatusResponse {
    private int totalAgents;
    private List<AgentStatusResponse> agents;
}
