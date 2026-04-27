package com.njhtr.marltsc.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private String intersectionId;
    private String alertType;
    private Integer severity;
    private Long timestamp;
    private String message;
}
