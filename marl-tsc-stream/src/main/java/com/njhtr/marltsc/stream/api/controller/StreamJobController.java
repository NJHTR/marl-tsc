package com.njhtr.marltsc.stream.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.stream.job.TrafficStreamJob;
import com.njhtr.marltsc.stream.manager.StreamJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamJobController {

    private final StreamJobManager streamJobManager;

    @GetMapping("/status")
    public ApiResult<Map<String, Object>> getStatus() {
        StreamJobManager.JobStatus status = streamJobManager.getStatus();
        return ApiResult.ok(Map.of(
                "service", "stream-processing-service",
                "job", TrafficStreamJob.JOB_NAME,
                "status", status.name()));
    }

    @PostMapping("/start")
    public ApiResult<Map<String, Object>> startJob() {
        streamJobManager.startJobs();
        return ApiResult.ok(Map.of(
                "message", "Flink job submitted",
                "status", streamJobManager.getStatus().name()));
    }

    @PostMapping("/stop")
    public ApiResult<Map<String, Object>> stopJob() {
        streamJobManager.stopJobs();
        return ApiResult.ok(Map.of(
                "message", "Flink job stop requested",
                "status", streamJobManager.getStatus().name()));
    }

    @GetMapping("/metrics")
    public ApiResult<Map<String, Object>> getMetrics() {
        return ApiResult.ok(Map.of(
                "recordsProcessed", 0,
                "averageLatencyMs", 0,
                "status", streamJobManager.getStatus().name()));
    }
}
