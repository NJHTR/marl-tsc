package com.njhtr.marltsc.web.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.infrastructure.client.dto.ActionResponse;
import com.njhtr.marltsc.web.infrastructure.client.dto.InferenceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "drl-engine-service", url = "http://localhost:8084")
public interface DrlEngineClient {

    @PostMapping("/api/v1/drl/decide")
    ApiResult<ActionResponse> inferAction(@RequestBody InferenceRequest request);
}
