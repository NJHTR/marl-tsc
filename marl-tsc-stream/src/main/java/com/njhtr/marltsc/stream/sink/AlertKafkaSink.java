package com.njhtr.marltsc.stream.sink;

import com.njhtr.marltsc.stream.config.KafkaConfig;
import com.njhtr.marltsc.stream.function.AlertEventSerializationSchema;
import com.njhtr.marltsc.stream.model.AlertEvent;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

public class AlertKafkaSink {

    public static FlinkKafkaProducer<AlertEvent> create(KafkaConfig config) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getBootstrapServers());

        return new FlinkKafkaProducer<>(
                config.getTopics().getOutputAlerts(),
                new AlertEventSerializationSchema(),
                props,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE
        );
    }
}
