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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutePlanningAppServiceImpl implements RoutePlanningAppService {

    private final IntersectionRepository intersectionRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RouteOptimizationDomainService routeOptimizationDomainService;

    @Override
    public RouteResponse computeRoute(RouteRequest request) {
        Map<String, IntersectionNode> nodes = loadNodes();
        List<RoadSegment> segments = roadSegmentRepository.findAll();
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
        List<RoadSegment> segments = roadSegmentRepository.findAll();
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
        List<RoadSegment> segments = roadSegmentRepository.findAll();
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
        return intersectionRepository.findAll().stream()
                .collect(Collectors.toMap(IntersectionNode::getId, n -> n));
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
