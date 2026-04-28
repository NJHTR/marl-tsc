package com.njhtr.marltsc.drl.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.drl.api.dto.request.InferenceRequest;
import com.njhtr.marltsc.drl.api.dto.request.TrainRequest;
import com.njhtr.marltsc.drl.api.dto.response.ActionResponse;
import com.njhtr.marltsc.drl.api.dto.response.AgentStatusResponse;
import com.njhtr.marltsc.drl.api.dto.response.TrainingStatusResponse;
import com.njhtr.marltsc.drl.domain.service.InferenceService;
import com.njhtr.marltsc.drl.domain.service.TrainingService;
import com.njhtr.marltsc.drl.infrastructure.kafka.TrafficStateHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/drl")
@RequiredArgsConstructor
public class DrlEngineController {

    private final InferenceService inferenceService;
    private final TrainingService trainingService;
    private final TrafficStateHolder trafficStateHolder;

    @PostMapping("/decide")
    public ApiResult<ActionResponse> decide(@RequestBody InferenceRequest request) {
        ActionResponse response = inferenceService.decide(request);
        return ApiResult.ok(response);
    }

    @PostMapping("/train")
    public ApiResult<ActionResponse> train(@RequestBody TrainRequest request) {
        ActionResponse response = trainingService.trainStep(request);
        return ApiResult.ok(response);
    }

    @GetMapping("/agents")
    public ApiResult<TrainingStatusResponse> getAgents() {
        return ApiResult.ok(trainingService.getAllAgentStatus());
    }

    @GetMapping("/agents/{intersectionId}")
    public ApiResult<AgentStatusResponse> getAgent(@PathVariable String intersectionId) {
        TrainingStatusResponse all = trainingService.getAllAgentStatus();
        return all.getAgents().stream()
                .filter(a -> a.getIntersectionId().equals(intersectionId))
                .findFirst()
                .map(ApiResult::ok)
                .orElse(ApiResult.fail(404, "Agent not found: " + intersectionId));
    }

    @PostMapping("/agents/{intersectionId}/toggle-training")
    public ApiResult<Void> toggleTraining(
            @PathVariable String intersectionId,
            @RequestParam boolean enabled) {
        trainingService.setTrainingMode(intersectionId, enabled);
        return ApiResult.ok();
    }

    @PostMapping("/agents/{intersectionId}/reset")
    public ApiResult<Void> resetAgent(@PathVariable String intersectionId) {
        trainingService.resetAgent(intersectionId);
        return ApiResult.ok();
    }

    @GetMapping("/traffic-state/{intersectionId}")
    public ApiResult<TrafficStateHolder.TrafficFeatureSnapshot> getTrafficState(
            @PathVariable String intersectionId) {
        TrafficStateHolder.TrafficFeatureSnapshot state = trafficStateHolder.get(intersectionId);
        if (state == null) {
            return ApiResult.fail(404, "No traffic state for: " + intersectionId);
        }
        return ApiResult.ok(state);
    }

    @GetMapping("/traffic-states")
    public ApiResult<Map<String, TrafficStateHolder.TrafficFeatureSnapshot>> getAllTrafficStates() {
        return ApiResult.ok(trafficStateHolder.getAll());
    }
}
