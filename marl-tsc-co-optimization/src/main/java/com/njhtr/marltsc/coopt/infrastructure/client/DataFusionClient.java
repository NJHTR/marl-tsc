package com.njhtr.marltsc.coopt.infrastructure.client;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.IntersectionInfoResponse;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.TrafficSnapshotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "data-fusion-service", url = "http://localhost:8085")
public interface DataFusionClient {

    @GetMapping("/api/v1/fusion/simulation/intersections")
    ApiResult<List<IntersectionInfoResponse>> listIntersections();

    @GetMapping("/api/v1/fusion/simulation/all")
    ApiResult<List<TrafficSnapshotResponse>> getAllSnapshots();

    @GetMapping("/api/v1/fusion/simulation/{intersectionId}")
    ApiResult<TrafficSnapshotResponse> getSnapshot(
            @org.springframework.web.bind.annotation.PathVariable("intersectionId") String intersectionId);
}
