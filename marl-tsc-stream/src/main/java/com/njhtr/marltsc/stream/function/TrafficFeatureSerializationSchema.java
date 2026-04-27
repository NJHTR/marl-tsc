package com.njhtr.marltsc.stream.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;

public class TrafficFeatureSerializationSchema
        implements KeyedSerializationSchema<TrafficFlowFeature> {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serializeKey(TrafficFlowFeature element) {
        return element.getIntersectionId() != null
                ? element.getIntersectionId().getBytes()
                : "unknown".getBytes();
    }

    @Override
    public byte[] serializeValue(TrafficFlowFeature element) {
        try {
            return mapper.writeValueAsBytes(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize TrafficFlowFeature", e);
        }
    }

    @Override
    public String getTargetTopic(TrafficFlowFeature element) {
        return null; // use producer-level default topic
    }
}
