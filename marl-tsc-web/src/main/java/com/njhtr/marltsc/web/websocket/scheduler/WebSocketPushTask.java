package com.njhtr.marltsc.web.websocket.scheduler;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.infrastructure.client.DataFusionClient;
import com.njhtr.marltsc.web.infrastructure.client.SignalControlClient;
import com.njhtr.marltsc.common.dto.SignalPlanResponse;
import com.njhtr.marltsc.web.infrastructure.client.dto.TrafficFeatureResponse;
import com.njhtr.marltsc.web.websocket.AlertMessage;
import com.njhtr.marltsc.web.websocket.SignalUpdateMessage;
import com.njhtr.marltsc.web.websocket.TrafficUpdateMessage;
import com.njhtr.marltsc.web.websocket.TrafficWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPushTask {

    private static final List<String> MONITORED_INTERSECTIONS = List.of("INT-001", "INT-002", "INT-003", "INT-004");

    private final DataFusionClient dataFusionClient;
    private final SignalControlClient signalControlClient;
    private final TrafficWebSocketHandler webSocketHandler;

    @Scheduled(fixedRate = 3000)
    public void pushUpdates() {
        for (String intersectionId : MONITORED_INTERSECTIONS) {
            pushTrafficUpdate(intersectionId);
            pushSignalUpdate(intersectionId);
        }
    }

    private void pushTrafficUpdate(String intersectionId) {
        try {
            ApiResult<TrafficFeatureResponse> result = dataFusionClient.getFeature(intersectionId);
            if (result != null && result.getData() != null) {
                TrafficFeatureResponse feature = result.getData();

                TrafficUpdateMessage msg = new TrafficUpdateMessage();
                msg.setIntersectionId(feature.getIntersectionId());
                msg.setFlow(feature.getFlow());
                msg.setSpeed(feature.getSpeed());
                msg.setOccupancy(feature.getOccupancy());
                msg.setTimestamp(feature.getTimestamp() != null ? feature.getTimestamp() : System.currentTimeMillis());
                webSocketHandler.broadcastTrafficUpdate(msg);

                if (feature.getOccupancy() != null && feature.getOccupancy() > 0.8) {
                    AlertMessage alert = new AlertMessage();
                    alert.setIntersectionId(intersectionId);
                    alert.setAlertType("HIGH_OCCUPANCY");
                    alert.setSeverity(2);
                    alert.setMessage("路口 " + intersectionId + " 占用率过高: " + feature.getOccupancy());
                    alert.setTimestamp(System.currentTimeMillis());
                    webSocketHandler.broadcastAlert(alert);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to push traffic update for {}: {}", intersectionId, e.getMessage());
        }
    }

    private void pushSignalUpdate(String intersectionId) {
        try {
            ApiResult<SignalPlanResponse> result = signalControlClient.getPlan(intersectionId);
            if (result != null && result.getData() != null) {
                SignalPlanResponse plan = result.getData();

                SignalUpdateMessage msg = new SignalUpdateMessage();
                msg.setIntersectionId(intersectionId);
                msg.setCurrentPhase(1);
                msg.setGreenTime(plan.getCycleTime());
                msg.setStatus("running");
                webSocketHandler.broadcastSignalUpdate(msg);
            }
        } catch (Exception e) {
            log.debug("Failed to push signal update for {}: {}", intersectionId, e.getMessage());
        }
    }
}
