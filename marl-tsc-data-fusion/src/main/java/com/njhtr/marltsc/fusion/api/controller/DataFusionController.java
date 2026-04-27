package com.njhtr.marltsc.fusion.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.fusion.api.dto.request.DataIngestRequest;
import com.njhtr.marltsc.fusion.api.dto.response.FusionResultResponse;
import com.njhtr.marltsc.fusion.api.dto.response.StateVectorResponse;
import com.njhtr.marltsc.fusion.api.dto.response.TrafficFeatureResponse;
import com.njhtr.marltsc.fusion.domain.bo.StateVectorBO;
import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;
import com.njhtr.marltsc.fusion.service.api.DataFusionAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/fusion")
@RequiredArgsConstructor
public class DataFusionController {

    private final DataFusionAppService dataFusionAppService;

    @PostMapping("/ingest")
    public ApiResult<FusionResultResponse> ingest(@RequestBody DataIngestRequest request) {
        dataFusionAppService.ingestRawData(request);

        FusionResultResponse response = new FusionResultResponse();
        response.setSourceType(request.getSourceType());
        response.setIntersectionId(request.getIntersectionId());
        response.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : System.currentTimeMillis());
        return ApiResult.ok(response);
    }

    @GetMapping("/features/{intersectionId}")
    public ApiResult<TrafficFeatureResponse> getFeatures(@PathVariable String intersectionId) {
        TrafficFlowFeature feature = dataFusionAppService.getLatestFeature(intersectionId);
        if (feature == null) {
            return ApiResult.fail(404, "No features found for intersection: " + intersectionId);
        }
        TrafficFeatureResponse response = new TrafficFeatureResponse();
        response.setIntersectionId(feature.getIntersectionId());
        response.setTimestamp(feature.getTimestamp());
        response.setFlow(feature.getFlow());
        response.setSpeed(feature.getSpeed());
        response.setOccupancy(feature.getOccupancy());
        response.setQueueLength(feature.getQueueLength());
        response.setDelay(feature.getDelay());
        return ApiResult.ok(response);
    }

    @GetMapping("/state/{intersectionId}")
    public ApiResult<StateVectorResponse> getStateVector(@PathVariable String intersectionId) {
        StateVectorBO stateVector = dataFusionAppService.getLatestStateVector(intersectionId);
        if (stateVector == null) {
            return ApiResult.fail(404, "No state vector found for intersection: " + intersectionId);
        }
        StateVectorResponse response = new StateVectorResponse();
        response.setIntersectionId(stateVector.getIntersectionId());
        response.setTimestamp(stateVector.getTimestamp());
        response.setValues(Arrays.stream(stateVector.getValues()).boxed().toList());
        return ApiResult.ok(response);
    }
}
