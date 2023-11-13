package com.techcat.feline.kafka;

import com.techcat.feline.config.KafkaEnvConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Properties;

@Slf4j
public class KafkaClientFactory implements AutoCloseable {
    private final AdminClient adminClient;
    private final KafkaProducer<Object, Object> producer;
    private final KafkaConsumer<Object, Object> consumer;

    public KafkaClientFactory() {
        Properties kafkaProperties = new KafkaEnvConfigLoader().loadPropertiesFromEnvironment();

        adminClient = AdminClient.create(kafkaProperties);
        producer = new KafkaProducer<>(kafkaProperties);
        consumer = new KafkaConsumer<>(kafkaProperties);
    }

    public AdminClient kafkaAdmin() {
        return adminClient;
    }

    public KafkaProducer<Object, Object> kafkaProducer() {
        return producer;
    }

    public KafkaConsumer<Object, Object> kafkaConsumer() {
        return consumer;
    }

    @Override
    public void close() {
        try {
            adminClient.close();
            producer.close();
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
