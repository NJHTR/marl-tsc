package com.njhtr.marltsc.fusion.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.fusion.api.dto.response.IntersectionInfoResponse;
import com.njhtr.marltsc.fusion.api.dto.response.TrafficSnapshotResponse;
import com.njhtr.marltsc.fusion.domain.service.TrafficSimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fusion/simulation")
@RequiredArgsConstructor
public class TrafficSimulationController {

    private final TrafficSimulationEngine simulationEngine;

    /** Realistic intersection metadata: based on Shanghai central grid (31.23N, 121.47E) */
    private static final List<IntersectionInfoResponse> INTERSECTIONS = buildIntersectionMetadata();

    /** Cache the last snapshot per intersection so rapid polling returns stable data */
    private final Map<String, TrafficSnapshotResponse> snapshotCache = new ConcurrentHashMap<>();

    private static List<IntersectionInfoResponse> buildIntersectionMetadata() {
        // Center point: approximate Shanghai People's Square
        double baseLat = 31.2304;
        double baseLng = 121.4737;
        double step = 0.005; // ~500m grid spacing

        List<IntersectionInfoResponse> list = new ArrayList<>();
        // 3x3 grid: row = north-south, col = east-west
        String[] names = {"INT-001", "INT-002", "INT-003", "INT-004", "INT-005",
                          "INT-006", "INT-007", "INT-008", "INT-009"};
        int[][] gridPos = {
            {1, 1}, {1, 2}, {1, 0},  // row 0 (north): INT-004,-005,-006
            {2, 1}, {2, 2}, {2, 0},  // row 1 (center): INT-001,-002,-003
            {0, 1}, {0, 2}, {0, 0}   // row 2 (south): INT-007,-008,-009
        };
        int[] cap = {1700, 1800, 1600, 1800, 1900, 1700, 1600, 1700, 1500};

        for (int i = 0; i < names.length; i++) {
            int row = gridPos[i][0] - 1; // -1, 0, 1
            int col = gridPos[i][1] - 1;
            double lat = baseLat + row * step;
            double lng = baseLng + col * step;

            list.add(IntersectionInfoResponse.builder()
                    .intersectionId(names[i])
                    .name(names[i].replace("INT-", "路口") + "号")
                    .latitude(Math.round(lat * 10000) / 10000.0)
                    .longitude(Math.round(lng * 10000) / 10000.0)
                    .capacity(cap[i])
                    .approaches(List.of(
                        approach("N", 3, 180, 3.5),
                        approach("S", 3, 180, 3.5),
                        approach("E", 3, 180, 3.5),
                        approach("W", 3, 180, 3.5)
                    ))
                    .build());
        }
        return list;
    }

    private static IntersectionInfoResponse.RoadApproach approach(String dir, int lanes, double len, double width) {
        return IntersectionInfoResponse.RoadApproach.builder()
                .direction(dir).lanes(lanes).roadLength(len).roadWidth(width).build();
    }

    @GetMapping("/intersections")
    public ApiResult<List<IntersectionInfoResponse>> listIntersections() {
        return ApiResult.ok(INTERSECTIONS);
    }

    @GetMapping("/all")
    public ApiResult<List<TrafficSnapshotResponse>> getAllSnapshots() {
        LocalDateTime now = LocalDateTime.now();
        List<TrafficSnapshotResponse> result = simulationEngine.generateAll(now).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        // Update cache
        for (TrafficSnapshotResponse r : result) {
            snapshotCache.put(r.getIntersectionId(), r);
        }
        return ApiResult.ok(result);
    }

    @GetMapping("/{intersectionId}")
    public ApiResult<TrafficSnapshotResponse> getSnapshot(@PathVariable String intersectionId) {
        LocalDateTime now = LocalDateTime.now();
        TrafficSimulationEngine.TrafficSnapshot snap = simulationEngine.generate(intersectionId, now);
        TrafficSnapshotResponse resp = toResponse(snap);
        snapshotCache.put(intersectionId, resp);
        return ApiResult.ok(resp);
    }

    @GetMapping("/intersection/{intersectionId}")
    public ApiResult<IntersectionInfoResponse> getIntersectionInfo(@PathVariable String intersectionId) {
        return INTERSECTIONS.stream()
                .filter(i -> i.getIntersectionId().equals(intersectionId))
                .findFirst()
                .map(ApiResult::ok)
                .orElseGet(() -> ApiResult.fail(404, "Intersection not found: " + intersectionId));
    }

    private TrafficSnapshotResponse toResponse(TrafficSimulationEngine.TrafficSnapshot snap) {
        TrafficSnapshotResponse r = new TrafficSnapshotResponse();
        r.setIntersectionId(snap.getIntersectionId());
        r.setFlow(snap.getFlow());
        r.setSpeed(snap.getSpeed());
        r.setOccupancy(snap.getOccupancy());
        r.setQueueLength(snap.getQueueLength());
        r.setDelay(snap.getDelay());
        r.setTimestamp(System.currentTimeMillis());
        r.setCongestionLevel(snap.getCongestionLevel());
        return r;
    }
}
