package com.njhtr.marltsc.stream.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.stream.model.AlertEvent;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;

public class AlertEventSerializationSchema
        implements KeyedSerializationSchema<AlertEvent> {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serializeKey(AlertEvent element) {
        return element.getIntersectionId().getBytes();
    }

    @Override
    public byte[] serializeValue(AlertEvent element) {
        try {
            return mapper.writeValueAsBytes(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize AlertEvent", e);
        }
    }

    @Override
    public String getTargetTopic(AlertEvent element) {
        return null; // use producer-level default topic
    }
}
