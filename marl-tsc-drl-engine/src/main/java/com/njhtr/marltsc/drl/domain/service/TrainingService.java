package com.njhtr.marltsc.drl.domain.service;

import com.njhtr.marltsc.drl.api.dto.request.TrainRequest;
import com.njhtr.marltsc.drl.api.dto.response.ActionResponse;
import com.njhtr.marltsc.drl.api.dto.response.AgentStatusResponse;
import com.njhtr.marltsc.drl.api.dto.response.TrainingStatusResponse;
import com.njhtr.marltsc.drl.domain.agent.AgentManager;
import com.njhtr.marltsc.drl.domain.agent.TrafficSignalAgent;
import com.njhtr.marltsc.drl.domain.config.DrlConfig;
import com.njhtr.marltsc.drl.domain.experience.Experience;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final AgentManager agentManager;
    private final DrlConfig config;

    public ActionResponse trainStep(TrainRequest request) {
        TrafficSignalAgent agent = agentManager.getOrCreateAgent(request.getIntersectionId());

        Experience experience = new Experience(
                request.getState(), request.getAction(),
                request.getReward(), request.getNextState(), request.isDone()
        );
        agent.train(experience);

        double[] qValues = agent.predictQValues(request.getState());
        double maxQ = Double.NEGATIVE_INFINITY;
        for (double q : qValues) {
            if (q > maxQ) maxQ = q;
        }
        double confidence = Math.min(1.0, Math.max(0.0, (maxQ + 10.0) / 20.0));

        int action = agent.selectAction(request.getState());
        return new ActionResponse(request.getIntersectionId(), action, qValues, confidence);
    }

    public TrainingStatusResponse getAllAgentStatus() {
        List<AgentStatusResponse> agents = agentManager.getAllAgents().stream()
                .map(a -> new AgentStatusResponse(
                        a.getIntersectionId(), a.isTrainingMode(),
                        a.getEpsilon(), a.getReplayBufferSize(), a.getTotalSteps()))
                .toList();
        return new TrainingStatusResponse(agents.size(), agents);
    }

    public void setTrainingMode(String intersectionId, boolean trainingMode) {
        TrafficSignalAgent agent = agentManager.getOrCreateAgent(intersectionId);
        agent.setTrainingMode(trainingMode);
        log.info("Agent {} training mode set to {}", intersectionId, trainingMode);
    }

    public void resetAgent(String intersectionId) {
        TrafficSignalAgent agent = agentManager.getAgent(intersectionId);
        if (agent != null) {
            agent.reset();
            log.info("Agent {} reset", intersectionId);
        }
    }
}
