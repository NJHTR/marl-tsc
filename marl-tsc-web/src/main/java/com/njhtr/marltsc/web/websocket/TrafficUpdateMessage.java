package com.njhtr.marltsc.web.websocket;

import lombok.Data;

@Data
public class TrafficUpdateMessage {
    private String intersectionId;
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Long timestamp;
}
