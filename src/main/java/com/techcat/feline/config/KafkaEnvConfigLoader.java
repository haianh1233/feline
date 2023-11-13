package com.techcat.feline.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Slf4j
public class KafkaEnvConfigLoader {
    private static final String PREFIX = "KAFKA_";
    private static final Map<String, String> defaultConfigs = Map.of(
            "bootstrap.servers", "localhost:9092",
            "schema.registry.url", "http://localhost:8081",
            "ksql.queries.file", "./config/ksql_queries.sql"
    );

    public Properties loadPropertiesFromEnvironment() {
        log.info("Loading Kafka Config from environment vars with prefix: " + PREFIX);
        final Properties cfg = new Properties();

        System.getenv().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(PREFIX))
                .forEach(entry -> {
                    var kafkaKey = entry.getKey().substring(PREFIX.length())
                            .replace("_", ".")
                            .toLowerCase();

                    log.info("Adding Kafka config: [{}] = [{}] via [{}] env variable", kafkaKey, entry.getValue(), kafkaKey);
                    cfg.put(kafkaKey, entry.getValue());
                });

        loadDefaultConfigs(cfg);
        loadClientConfigs(cfg);
        return cfg;
    }

    private void loadDefaultConfigs(Properties properties) {
        defaultConfigs.forEach((key, value) -> {
            if (!properties.containsKey(key)) {
                log.info("Kafka env variable for [{}] not found - add default config value [{}]", key, value);
                properties.put(key, value);
            }
        });
    }

    private void loadClientConfigs(Properties properties) {
        properties.put(GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        properties.put(KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
    }
}
