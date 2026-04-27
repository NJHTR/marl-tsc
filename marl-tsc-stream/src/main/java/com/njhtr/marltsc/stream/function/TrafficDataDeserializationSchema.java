package com.njhtr.marltsc.stream.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.stream.model.UnifiedTrafficData;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

public class TrafficDataDeserializationSchema implements DeserializationSchema<UnifiedTrafficData> {
    private static final long serialVersionUID = 1L;
    private transient ObjectMapper mapper;

    @Override
    public UnifiedTrafficData deserialize(byte[] message) throws IOException {
        if (message == null) return null;
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
        }
        UnifiedTrafficData data = mapper.readValue(message, UnifiedTrafficData.class);
        return data.getIntersectionId() != null ? data : null;
    }

    @Override
    public boolean isEndOfStream(UnifiedTrafficData nextElement) {
        return false;
    }

    @Override
    public TypeInformation<UnifiedTrafficData> getProducedType() {
        return TypeInformation.of(UnifiedTrafficData.class);
    }
}
