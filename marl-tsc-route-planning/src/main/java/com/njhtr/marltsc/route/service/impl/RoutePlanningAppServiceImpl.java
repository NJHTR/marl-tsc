package com.njhtr.marltsc.route.service.impl;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.route.api.dto.request.RouteRequest;
import com.njhtr.marltsc.route.api.dto.response.PathNodeResponse;
import com.njhtr.marltsc.route.api.dto.response.RouteResponse;
import com.njhtr.marltsc.route.domain.entity.IntersectionNode;
import com.njhtr.marltsc.route.domain.entity.RoadSegment;
import com.njhtr.marltsc.route.domain.service.RouteOptimizationDomainService;
import com.njhtr.marltsc.route.infrastructure.repository.IntersectionRepository;
import com.njhtr.marltsc.route.infrastructure.repository.RoadSegmentRepository;
import com.njhtr.marltsc.route.service.api.RoutePlanningAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePlanningAppServiceImpl implements RoutePlanningAppService {

    private final IntersectionRepository intersectionRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RouteOptimizationDomainService routeOptimizationDomainService;

    @Override
    public RouteResponse computeRoute(RouteRequest request) {
        Map<String, IntersectionNode> nodes = loadNodes();
        List<RoadSegment> segments = loadSegments();
        Map<String, List<RoadSegment>> adjacencyList = buildAdjacencyList(segments);
        Map<String, Double> predictedTravelTimes = buildTravelTimeMap(segments);

        List<String> path = routeOptimizationDomainService.computeOptimalPath(
                request.getOriginId(), request.getDestinationId(),
                predictedTravelTimes, nodes, adjacencyList);

        if (path.isEmpty()) {
            throw new BusinessException(400, "No viable route found from " + request.getOriginId() + " to " + request.getDestinationId());
        }

        return assembleRouteResponse(path, nodes, segments, request);
    }

    @Override
    public List<RouteResponse> computeAlternativeRoutes(RouteRequest request) {
        Map<String, IntersectionNode> nodes = loadNodes();
        List<RoadSegment> segments = loadSegments();
        Map<String, List<RoadSegment>> adjacencyList = buildAdjacencyList(segments);
        Map<String, Double> predictedTravelTimes = buildTravelTimeMap(segments);

        List<String> optimalPath = routeOptimizationDomainService.computeOptimalPath(
                request.getOriginId(), request.getDestinationId(),
                predictedTravelTimes, nodes, adjacencyList);

        if (optimalPath.isEmpty()) {
            return Collections.emptyList();
        }

        List<RouteResponse> alternatives = new ArrayList<>();
        alternatives.add(assembleRouteResponse(optimalPath, nodes, segments, request));

        Set<String> excludedSegments = new HashSet<>();
        for (int i = 0; i < optimalPath.size() - 1 && alternatives.size() < 3; i++) {
            String fromId = optimalPath.get(i);
            String toId = optimalPath.get(i + 1);

            String segmentId = findSegmentId(fromId, toId, adjacencyList);
            if (segmentId == null || excludedSegments.contains(segmentId)) {
                continue;
            }
            excludedSegments.add(segmentId);

            double originalTime = predictedTravelTimes.getOrDefault(segmentId, Double.MAX_VALUE);
            predictedTravelTimes.put(segmentId, Double.MAX_VALUE);

            List<String> altPath = routeOptimizationDomainService.computeOptimalPath(
                    request.getOriginId(), request.getDestinationId(),
                    predictedTravelTimes, nodes, adjacencyList);

            predictedTravelTimes.put(segmentId, originalTime);

            if (!altPath.isEmpty() && !altPath.equals(optimalPath)) {
                alternatives.add(assembleRouteResponse(altPath, nodes, segments, request));
            }
        }

        return alternatives;
    }

    @Override
    public Map<String, Double> getNetworkStatus() {
        List<RoadSegment> segments = loadSegments();
        Map<String, Double> status = new HashMap<>();
        for (RoadSegment segment : segments) {
            Double travelTime = segment.getCurrentTravelTime();
            if (travelTime == null || travelTime <= 0) {
                travelTime = segment.getLength() != null && segment.getFreeFlowSpeed() != null && segment.getFreeFlowSpeed() > 0
                        ? segment.getLength() / segment.getFreeFlowSpeed()
                        : 0.0;
            }
            status.put(String.valueOf(segment.getId()), travelTime);
        }
        return status;
    }

    private Map<String, IntersectionNode> loadNodes() {
        try {
            return intersectionRepository.findAll().stream()
                    .collect(Collectors.toMap(IntersectionNode::getId, n -> n));
        } catch (Exception e) {
            log.warn("Neo4j unavailable, using demo intersection data: {}", e.getMessage());
            return getDemoNodes();
        }
    }

    private List<RoadSegment> loadSegments() {
        try {
            return roadSegmentRepository.findAll();
        } catch (Exception e) {
            log.warn("Neo4j unavailable, using demo road segment data: {}", e.getMessage());
            return getDemoSegments();
        }
    }

    private static Map<String, IntersectionNode> getDemoNodes() {
        Map<String, IntersectionNode> nodes = new LinkedHashMap<>();
        double baseLat = 31.2304;
        double baseLng = 121.4737;

        for (int i = 1; i <= 9; i++) {
            IntersectionNode node = new IntersectionNode();
            String id = "INT-00" + i;
            node.setId(id);
            node.setName(id + "路口");
            int row = (i - 1) / 3;
            int col = (i - 1) % 3;
            node.setLatitude(baseLat + row * 0.01);
            node.setLongitude(baseLng + col * 0.01);
            nodes.put(id, node);
        }
        return nodes;
    }

    private static List<RoadSegment> getDemoSegments() {
        List<RoadSegment> segments = new ArrayList<>();
        // Grid connections: each intersection connects to right and bottom neighbors
        long id = 1;
        for (int i = 1; i <= 9; i++) {
            String from = "INT-00" + i;
            int row = (i - 1) / 3;
            int col = (i - 1) % 3;

            if (col < 2) {
                String to = "INT-00" + (i + 1);
                segments.add(newSegment(id++, from, to, 500, 40, 30));
            }
            if (row < 2) {
                String to = "INT-00" + (i + 3);
                segments.add(newSegment(id++, from, to, 600, 40, 36));
            }
        }
        return segments;
    }

    private static RoadSegment newSegment(long id, String from, String to, double length,
                                          double freeFlowSpeed, double currentSpeed) {
        RoadSegment seg = new RoadSegment();
        seg.setId(id);
        seg.setFromId(from);
        seg.setToId(to);
        seg.setLength(length);
        seg.setFreeFlowSpeed(freeFlowSpeed / 3.6); // km/h to m/s
        seg.setCurrentSpeed(currentSpeed / 3.6);
        seg.setCurrentTravelTime(length / (currentSpeed / 3.6));
        seg.setLanes(3);
        return seg;
    }

    private Map<String, List<RoadSegment>> buildAdjacencyList(List<RoadSegment> segments) {
        return segments.stream()
                .filter(s -> s.getFromId() != null)
                .collect(Collectors.groupingBy(RoadSegment::getFromId));
    }

    private Map<String, Double> buildTravelTimeMap(List<RoadSegment> segments) {
        Map<String, Double> map = new HashMap<>();
        for (RoadSegment s : segments) {
            double time = (s.getCurrentTravelTime() != null) ? s.getCurrentTravelTime()
                    : (s.getLength() != null && s.getFreeFlowSpeed() != null && s.getFreeFlowSpeed() > 0)
                    ? s.getLength() / s.getFreeFlowSpeed()
                    : Double.MAX_VALUE;
            map.put(String.valueOf(s.getId()), time);
        }
        return map;
    }

    private String findSegmentId(String fromId, String toId, Map<String, List<RoadSegment>> adjacencyList) {
        return adjacencyList.getOrDefault(fromId, Collections.emptyList()).stream()
                .filter(s -> toId.equals(s.getToId()))
                .findFirst()
                .map(s -> String.valueOf(s.getId()))
                .orElse(null);
    }

    private RouteResponse assembleRouteResponse(List<String> pathIds, Map<String, IntersectionNode> nodes,
                                                List<RoadSegment> segments, RouteRequest request) {
        RouteResponse response = new RouteResponse();
        response.setRouteId(UUID.randomUUID().toString());
        response.setOriginId(request.getOriginId());
        response.setDestinationId(request.getDestinationId());

        List<PathNodeResponse> path = new ArrayList<>();
        double totalDistance = 0.0;
        double totalTime = 0.0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < pathIds.size(); i++) {
            String nodeId = pathIds.get(i);
            IntersectionNode node = nodes.get(nodeId);
            if (node == null) {
                continue;
            }

            PathNodeResponse pathNode = new PathNodeResponse();
            pathNode.setIntersectionId(nodeId);
            pathNode.setIntersectionName(node.getName());
            pathNode.setLongitude(node.getLongitude());
            pathNode.setLatitude(node.getLatitude());
            pathNode.setExpectedArrivalTime(now.plusSeconds((long) totalTime));
            path.add(pathNode);

            if (i < pathIds.size() - 1) {
                String nextId = pathIds.get(i + 1);
                for (RoadSegment seg : segments) {
                    if (nodeId.equals(seg.getFromId()) && nextId.equals(seg.getToId())) {
                        totalDistance += seg.getLength() != null ? seg.getLength() : 0.0;
                        totalTime += seg.getCurrentTravelTime() != null ? seg.getCurrentTravelTime()
                                : (seg.getLength() != null && seg.getFreeFlowSpeed() != null && seg.getFreeFlowSpeed() > 0)
                                ? seg.getLength() / seg.getFreeFlowSpeed()
                                : 0.0;
                        break;
                    }
                }
            }
        }

        response.setPath(path);
        response.setTotalDistance(totalDistance);
        response.setEstimatedTime(totalTime);
        return response;
    }
}
