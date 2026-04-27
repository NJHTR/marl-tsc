package com.njhtr.marltsc.stream.sink;

import com.njhtr.marltsc.stream.config.KafkaConfig;
import com.njhtr.marltsc.stream.function.TrafficFeatureSerializationSchema;
import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

public class FeatureKafkaSink {

    public static FlinkKafkaProducer<TrafficFlowFeature> create(KafkaConfig config) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getBootstrapServers());

        return new FlinkKafkaProducer<>(
                config.getTopics().getOutputFeatures(),
                new TrafficFeatureSerializationSchema(),
                props,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE
        );
    }
}
