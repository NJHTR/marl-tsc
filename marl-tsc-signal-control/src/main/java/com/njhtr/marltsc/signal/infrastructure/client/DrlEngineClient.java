package com.njhtr.marltsc.signal.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.signal.infrastructure.client.dto.DrlActionResponse;
import com.njhtr.marltsc.signal.infrastructure.client.dto.DrlInferenceRequest;
import com.njhtr.marltsc.signal.infrastructure.client.dto.DrlTrainRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Component
public class DrlEngineClient {

    private final RestTemplate restTemplate;
    private final String drlEngineUrl;

    public DrlEngineClient(RestTemplate restTemplate,
                           @Value("${drl.engine.url:http://localhost:8084}") String drlEngineUrl) {
        this.restTemplate = restTemplate;
        this.drlEngineUrl = drlEngineUrl;
    }

    public Optional<DrlActionResponse> decide(String intersectionId, double[] state) {
        try {
            DrlInferenceRequest req = new DrlInferenceRequest(intersectionId, state);
            ResponseEntity<ApiResult<DrlActionResponse>> resp = restTemplate.exchange(
                    drlEngineUrl + "/api/v1/drl/decide",
                    HttpMethod.POST,
                    new HttpEntity<>(req),
                    new ParameterizedTypeReference<ApiResult<DrlActionResponse>>() {}
            );
            if (resp.getBody() != null && resp.getBody().getCode() == 200) {
                return Optional.ofNullable(resp.getBody().getData());
            }
        } catch (Exception e) {
            log.warn("DRL decide failed for {}: {}", intersectionId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<DrlActionResponse> train(String intersectionId, double[] state,
                                              int action, double reward,
                                              double[] nextState, boolean done) {
        try {
            DrlTrainRequest req = new DrlTrainRequest(intersectionId, state, action, reward, nextState, done);
            ResponseEntity<ApiResult<DrlActionResponse>> resp = restTemplate.exchange(
                    drlEngineUrl + "/api/v1/drl/train",
                    HttpMethod.POST,
                    new HttpEntity<>(req),
                    new ParameterizedTypeReference<ApiResult<DrlActionResponse>>() {}
            );
            if (resp.getBody() != null && resp.getBody().getCode() == 200) {
                return Optional.ofNullable(resp.getBody().getData());
            }
        } catch (Exception e) {
            log.warn("DRL train failed for {}: {}", intersectionId, e.getMessage());
        }
        return Optional.empty();
    }
}
