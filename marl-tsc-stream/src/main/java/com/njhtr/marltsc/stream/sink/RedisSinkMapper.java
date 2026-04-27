package com.njhtr.marltsc.stream.sink;

import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder Redis sink — in production, writes the latest TrafficFlowFeature
 * per intersection to Redis for low-latency queries by other services.
 */
@Slf4j
public class RedisSinkMapper implements org.apache.flink.streaming.api.functions.sink.SinkFunction<TrafficFlowFeature> {

    private static final long serialVersionUID = 1L;

    @Override
    public void invoke(TrafficFlowFeature value, Context context) {
        log.debug("RedisSink[placeholder] would store intersection {}: {}",
                value.getIntersectionId(), value);
    }
}
