package com.njhtr.marltsc.stream.function;

import com.njhtr.marltsc.stream.model.TrafficFlowAccumulator;
import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import com.njhtr.marltsc.stream.model.UnifiedTrafficData;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.Map;

public class TrafficFlowAggregateFunction
        implements AggregateFunction<UnifiedTrafficData, TrafficFlowAccumulator, TrafficFlowFeature> {

    private static final long serialVersionUID = 1L;

    @Override
    public TrafficFlowAccumulator createAccumulator() {
        return new TrafficFlowAccumulator();
    }

    @Override
    public TrafficFlowAccumulator add(UnifiedTrafficData value, TrafficFlowAccumulator acc) {
        if (acc.getCount() == 0) {
            acc.setWindowStart(value.getTimestamp() != null ? value.getTimestamp() : System.currentTimeMillis());
        }
        acc.setWindowEnd(value.getTimestamp() != null ? value.getTimestamp() : System.currentTimeMillis());
        acc.setCount(acc.getCount() + 1);

        Map<String, Object> features = value.getFeatures();
        if (features != null) {
            acc.setSumFlow(acc.getSumFlow() + getDouble(features, "flow"));
            acc.setSumSpeed(acc.getSumSpeed() + getDouble(features, "speed"));
            acc.setSumOccupancy(acc.getSumOccupancy() + getDouble(features, "occupancy"));
            acc.setMaxQueueLength(Math.max(acc.getMaxQueueLength(), getDouble(features, "queueLength")));
        }
        return acc;
    }

    @Override
    public TrafficFlowFeature getResult(TrafficFlowAccumulator acc) {
        TrafficFlowFeature result = new TrafficFlowFeature();
        result.setTimestamp(acc.getWindowEnd());
        result.setFlow(acc.getSumFlow());
        result.setSpeed(acc.getCount() > 0 ? acc.getSumSpeed() / acc.getCount() : 0.0);
        result.setOccupancy(acc.getCount() > 0 ? acc.getSumOccupancy() / acc.getCount() : 0.0);
        result.setQueueLength(acc.getMaxQueueLength());
        result.setDelay(0.0);
        return result;
    }

    @Override
    public TrafficFlowAccumulator merge(TrafficFlowAccumulator a, TrafficFlowAccumulator b) {
        TrafficFlowAccumulator m = new TrafficFlowAccumulator();
        m.setCount(a.getCount() + b.getCount());
        m.setSumFlow(a.getSumFlow() + b.getSumFlow());
        m.setSumSpeed(a.getSumSpeed() + b.getSumSpeed());
        m.setSumOccupancy(a.getSumOccupancy() + b.getSumOccupancy());
        m.setMaxQueueLength(Math.max(a.getMaxQueueLength(), b.getMaxQueueLength()));
        m.setWindowStart(Math.min(a.getWindowStart(), b.getWindowStart()));
        m.setWindowEnd(Math.max(a.getWindowEnd(), b.getWindowEnd()));
        return m;
    }

    private static double getDouble(Map<String, Object> features, String key) {
        Object val = features.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
}
