package com.njhtr.marltsc.stream.function;

import com.njhtr.marltsc.stream.model.AlertEvent;
import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;

public class CongestionDetectionFunction
        extends KeyedProcessFunction<String, TrafficFlowFeature, AlertEvent> {

    private static final long serialVersionUID = 1L;
    private static final int HISTORY_SIZE = 10;
    private static final double THRESHOLD_MULTIPLIER = 3.0;

    private ListState<Double> recentOccupancy;

    @Override
    public void open(Configuration parameters) {
        ListStateDescriptor<Double> descriptor = new ListStateDescriptor<>(
                "recentOccupancy",
                TypeInformation.of(Double.class));
        recentOccupancy = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(TrafficFlowFeature value, Context ctx, Collector<AlertEvent> out) throws Exception {
        if (value.getOccupancy() == null) return;

        double currentOccupancy = value.getOccupancy();
        recentOccupancy.add(currentOccupancy);

        List<Double> all = new ArrayList<>();
        for (Double v : recentOccupancy.get()) {
            all.add(v);
        }

        if (all.size() > HISTORY_SIZE) {
            all = new ArrayList<>(all.subList(all.size() - HISTORY_SIZE, all.size()));
            recentOccupancy.update(all);
        }

        if (all.size() < 3) return;

        double mean = all.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = all.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);
        double threshold = mean + THRESHOLD_MULTIPLIER * stddev;

        if (currentOccupancy > threshold) {
            out.collect(new AlertEvent(
                    value.getIntersectionId(),
                    "CONGESTION",
                    2,
                    value.getTimestamp(),
                    String.format("占用率异常: 路口 %s 当前 %.2f 超过阈值 %.2f (均值 %.2f, 标准差 %.2f)",
                            value.getIntersectionId(), currentOccupancy, threshold, mean, stddev)));
        }
    }
}
