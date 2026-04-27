package com.njhtr.marltsc.route.domain.service;

import com.njhtr.marltsc.route.domain.entity.IntersectionNode;
import com.njhtr.marltsc.route.domain.entity.RoadSegment;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class RouteOptimizationDomainService {

    public List<String> computeOptimalPath(String originId, String destinationId,
                                           Map<String, Double> predictedTravelTimes,
                                           Map<String, IntersectionNode> nodes,
                                           Map<String, List<RoadSegment>> adjacencyList) {
        if (!nodes.containsKey(originId) || !nodes.containsKey(destinationId)) {
            return Collections.emptyList();
        }

        double maxSpeed = adjacencyList.values().stream()
                .flatMap(List::stream)
                .mapToDouble(s -> s.getFreeFlowSpeed() != null ? s.getFreeFlowSpeed() : 0.0)
                .filter(s -> s > 0)
                .max()
                .orElse(33.33);

        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();
        Map<String, String> cameFrom = new HashMap<>();

        PriorityQueue<String> openSet = new PriorityQueue<>(Comparator.comparingDouble(fScore::get));

        gScore.put(originId, 0.0);
        fScore.put(originId, heuristic(nodes.get(originId), nodes.get(destinationId), maxSpeed));
        openSet.add(originId);

        Set<String> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            String current = openSet.poll();

            if (current.equals(destinationId)) {
                return reconstructPath(cameFrom, destinationId);
            }

            if (closedSet.contains(current)) {
                continue;
            }

            closedSet.add(current);

            List<RoadSegment> neighbors = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (RoadSegment segment : neighbors) {
                String neighborId = segment.getToId();
                if (neighborId == null || closedSet.contains(neighborId)) {
                    continue;
                }

                double travelTime = predictedTravelTimes.getOrDefault(String.valueOf(segment.getId()),
                        (segment.getLength() != null && segment.getFreeFlowSpeed() != null && segment.getFreeFlowSpeed() > 0)
                                ? segment.getLength() / segment.getFreeFlowSpeed()
                                : Double.MAX_VALUE);

                double tentativeG = gScore.get(current) + travelTime;

                if (tentativeG < gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    cameFrom.put(neighborId, current);
                    gScore.put(neighborId, tentativeG);
                    fScore.put(neighborId, tentativeG + heuristic(nodes.get(neighborId), nodes.get(destinationId), maxSpeed));
                    openSet.add(neighborId);
                }
            }
        }

        return Collections.emptyList();
    }

    private double heuristic(IntersectionNode from, IntersectionNode to, double maxSpeed) {
        double distance = haversineDistance(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
        return distance / maxSpeed;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        LinkedList<String> path = new LinkedList<>();
        path.addFirst(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }
        return path;
    }
}
