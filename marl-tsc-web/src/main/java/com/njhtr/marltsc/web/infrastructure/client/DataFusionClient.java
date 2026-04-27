package com.njhtr.marltsc.web.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.infrastructure.client.dto.TrafficFeatureResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "data-fusion-service", url = "http://localhost:8085")
public interface DataFusionClient {

    @GetMapping("/api/v1/fusion/features/{intersectionId}")
    ApiResult<TrafficFeatureResponse> getFeature(@PathVariable("intersectionId") String intersectionId);
}
