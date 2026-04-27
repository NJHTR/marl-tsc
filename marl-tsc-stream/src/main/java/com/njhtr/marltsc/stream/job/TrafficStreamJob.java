package com.njhtr.marltsc.stream.job;

import com.njhtr.marltsc.stream.config.FlinkConfig;
import com.njhtr.marltsc.stream.config.KafkaConfig;
import com.njhtr.marltsc.stream.function.CongestionDetectionFunction;
import com.njhtr.marltsc.stream.function.TrafficDataDeserializationSchema;
import com.njhtr.marltsc.stream.function.TrafficFlowAggregateFunction;
import com.njhtr.marltsc.stream.model.AlertEvent;
import com.njhtr.marltsc.stream.model.TrafficFlowFeature;
import com.njhtr.marltsc.stream.model.UnifiedTrafficData;
import com.njhtr.marltsc.stream.sink.AlertKafkaSink;
import com.njhtr.marltsc.stream.sink.FeatureKafkaSink;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.time.Duration;
import java.util.Properties;

public class TrafficStreamJob {

    public static final String JOB_NAME = "TrafficStreamJob";

    public static StreamExecutionEnvironment createEnvironment(FlinkConfig config) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(config.getParallelism());
        env.enableCheckpointing(config.getCheckpointInterval());
        env.getCheckpointConfig()
                .setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        env.getCheckpointConfig().setCheckpointTimeout(60_000);
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        return env;
    }

    public static FlinkKafkaConsumer<UnifiedTrafficData> createKafkaConsumer(KafkaConfig config) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getBootstrapServers());
        props.setProperty("group.id", "stream-processor");

        return new FlinkKafkaConsumer<>(
                config.getTopics().getInput(),
                new TrafficDataDeserializationSchema(),
                props
        );
    }

    public static void buildPipeline(StreamExecutionEnvironment env, KafkaConfig kafkaConfig) {
        FlinkKafkaConsumer<UnifiedTrafficData> consumer = createKafkaConsumer(kafkaConfig);

        WatermarkStrategy<UnifiedTrafficData> watermark = WatermarkStrategy
                .<UnifiedTrafficData>forBoundedOutOfOrderness(Duration.ofSeconds(30))
                .withTimestampAssigner((data, ts) ->
                        data.getTimestamp() != null ? data.getTimestamp() : System.currentTimeMillis());

        DataStream<UnifiedTrafficData> rawStream = env
                .addSource(consumer, "traffic-raw-source")
                .assignTimestampsAndWatermarks(watermark);

        // Windowed feature aggregation
        DataStream<TrafficFlowFeature> featureStream = rawStream
                .keyBy(UnifiedTrafficData::getIntersectionId)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new TrafficFlowAggregateFunction())
                .name("feature-aggregation");

        featureStream.addSink(FeatureKafkaSink.create(kafkaConfig))
                .name("feature-kafka-sink");

        featureStream.addSink(new com.njhtr.marltsc.stream.sink.RedisSinkMapper())
                .name("redis-sink-placeholder");

        // Congestion detection
        DataStream<AlertEvent> alertStream = featureStream
                .keyBy(TrafficFlowFeature::getIntersectionId)
                .process(new CongestionDetectionFunction())
                .name("congestion-detection");

        alertStream.addSink(AlertKafkaSink.create(kafkaConfig))
                .name("alert-kafka-sink");
    }
}
