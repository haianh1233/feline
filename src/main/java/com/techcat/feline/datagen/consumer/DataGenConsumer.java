package com.techcat.feline.datagen.consumer;

import com.techcat.feline.datagen.TopicCacheManager;
import com.techcat.feline.datagen.model.Data;
import com.techcat.feline.datagen.queue.DataQueue;
import com.techcat.feline.kafka.KafkaClientFactory;
import com.techcat.feline.utils.AvroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DataGenConsumer implements Runnable {
    private final Map<String, Schema> schemaCache;
    private final Map<String, Integer> schemaIdCache;

    public DataGenConsumer(Map<String, Schema> schemaCache, Map<String, Integer> schemaIdCache) {
        this.schemaCache = schemaCache;
        this.schemaIdCache = schemaIdCache;
    }

    @Override
    public void run() {
        try (KafkaClientFactory kafkaClientFactory = new KafkaClientFactory()) {
            KafkaProducer<Object, Object> kafkaProducer = kafkaClientFactory.kafkaProducer();
            while (true) {
                try {
                    final Data data = DataQueue.getData();
                    final String topic = data.getTopic();
                    final Object key = data.getKey();
                    final Object value = data.getValue();

                    log.info("Sending data to [{}]: {} - {}", topic, key, value);
                    kafkaProducer.send(
                            createProducerRecord(topic, key, value),
                            (metadata, exception) -> {
                                if (exception != null) {
                                    log.error("Error when sending data to Kafka", exception);
                                    throw new RuntimeException(exception);
                                }

                                TopicCacheManager.addData(data.getTopic(), data);
                            }
                    );
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private ProducerRecord<Object, Object> createProducerRecord(String topic, Object key, Object value) {
        try {
            Schema schema = schemaCache.get(topic);
            Integer schemaId = schemaIdCache.get(topic);

            if (schema == null || schemaId == null) {
                throw new RuntimeException("Schema not found");
            }

            GenericRecord genericRecord = AvroUtils.convertToGenericRecord(value, schema.toString());

            return new ProducerRecord<>(
                    topic,
                    key.toString().getBytes(),
                    genericRecord
            );
        } catch (Exception e) {
            log.error("Error when craete producer record", e);
            throw new RuntimeException(e);
        }
    }
}
