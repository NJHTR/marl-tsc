package com.njhtr.marltsc.drl.domain.service;

import com.njhtr.marltsc.drl.api.dto.request.InferenceRequest;
import com.njhtr.marltsc.drl.api.dto.response.ActionResponse;
import com.njhtr.marltsc.drl.domain.agent.AgentManager;
import com.njhtr.marltsc.drl.domain.agent.TrafficSignalAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InferenceService {

    private final AgentManager agentManager;

    public ActionResponse decide(InferenceRequest request) {
        TrafficSignalAgent agent = agentManager.getOrCreateAgent(request.getIntersectionId());
        int action = agent.selectAction(request.getState());
        double[] qValues = agent.predictQValues(request.getState());

        double maxQ = Double.NEGATIVE_INFINITY;
        for (double q : qValues) {
            if (q > maxQ) maxQ = q;
        }
        double confidence = Math.min(1.0, Math.max(0.0, (maxQ + 10.0) / 20.0));

        return new ActionResponse(request.getIntersectionId(), action, qValues, confidence);
    }

    public Map<String, ActionResponse> batchDecide(Map<String, double[]> states) {
        return states.entrySet().stream()
                .map(entry -> {
                    InferenceRequest req = new InferenceRequest();
                    req.setIntersectionId(entry.getKey());
                    req.setState(entry.getValue());
                    return decide(req);
                })
                .collect(Collectors.toMap(ActionResponse::getIntersectionId, r -> r));
    }
}
