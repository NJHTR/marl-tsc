package com.njhtr.marltsc.coopt.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.common.dto.DrlActionResponse;
import com.njhtr.marltsc.common.dto.DrlInferenceRequest;
import com.njhtr.marltsc.common.dto.DrlTrainRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "drl-engine-service", url = "http://localhost:8084")
public interface DrlEngineClient {

    @PostMapping("/api/v1/drl/decide")
    ApiResult<DrlActionResponse> decide(@RequestBody DrlInferenceRequest request);

    @PostMapping("/api/v1/drl/train")
    ApiResult<DrlActionResponse> train(@RequestBody DrlTrainRequest request);
}
