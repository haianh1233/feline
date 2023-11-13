package com.techcat.feline.datagen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcat.feline.config.ApplicationEnvConfigLoader;
import com.techcat.feline.datagen.model.ConfigEntry;
import com.techcat.feline.kafka.KafkaClientFactory;
import com.techcat.feline.kafka.SchemaRegistryClientFactory;
import com.techcat.feline.utils.AvroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.techcat.feline.config.ApplicationConfig.CONFIG_FILE_PATH;

@Slf4j
public class DataGeneratorService {
    private final DataInterpreterService dataInterpreterService;
    private final ObjectMapper objectMapper;

    private final Map<String, Schema> schemaCache = new HashMap<>();
    private final Map<String, Integer> schemaIdCache = new HashMap<>();

    public DataGeneratorService() {
        this.dataInterpreterService = new DataInterpreterService();
        this.objectMapper = new ObjectMapper();
    }

    public void start() throws Exception {
        String configFilePath = new ApplicationEnvConfigLoader().getConfig(CONFIG_FILE_PATH);
        List<ConfigEntry> configs = objectMapper.readValue(Path.of(configFilePath).toFile(), new TypeReference<>() {});
        configs.forEach(this::registerSchema);

        ExecutorService executorService = Executors.newFixedThreadPool(configs.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);

        submitTasks(configs, completionService);
        handleTaskCompletion(completionService);
        shutdownExecutor(executorService);
    }

    private void submitTasks(List<ConfigEntry> configs, CompletionService<Void> completionService) {
        for (ConfigEntry config : configs) {
            completionService.submit(() -> {
                sendFakeDataToKafka(config);
                return null;
            });
        }
    }

    private static void handleTaskCompletion(CompletionService<Void> completionService) throws InterruptedException {
        try {
            Future<Void> firstCompleted = completionService.take();
            firstCompleted.get();
        } catch (ExecutionException e) {
            log.error("Error when sending fake data to Kafka", e);
            throw new RuntimeException(e);
        }
    }

    private static void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void sendFakeDataToKafka(ConfigEntry config) {
        try (KafkaClientFactory kafkaClientFactory = new KafkaClientFactory()) {
            KafkaProducer<Object, Object> kafkaProducer = kafkaClientFactory.kafkaProducer();
            while (true) {

                Map<String, Object> data = dataInterpreterService.interpretConfig(config);

                Object value = data.get("value");
                System.out.println(config.getTopic() + " " + value);
                kafkaProducer.send(
                        createProducerRecord(config.getTopic(), data.get("key"), value),
                        (metadata, exception) -> {
                            if (exception != null) {
                                log.error("Error when sending data to Kafka", exception);
                                throw new RuntimeException(exception);
                            }

                            dataInterpreterService.saveToCache(config.getTopic(), data);
                        }
                );

                if (config.getConfig() != null && config.getConfig().getThrottle() != null) {
                    try {
                        Thread.sleep(config.getConfig().getThrottle().getMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

        }
    }

    private void registerSchema(ConfigEntry configEntry) {
        String topic = configEntry.getTopic();

        try {
            // Interpret schema
            try (KafkaClientFactory kafkaClientFactory = new KafkaClientFactory()){
                Schema schema = dataInterpreterService.interpretSchemaFromConfig(configEntry);

                // Create topic
                kafkaClientFactory.kafkaAdmin()
                        .createTopics(List.of(new NewTopic(topic, 1, (short) 1)));
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));

                // Register schema
                int schemaId = SchemaRegistryClientFactory.schemaRegistryClient()
                        .register(topic + "-value", schema);

                log.info("Schema: {}", schema);
                log.info("Registered schema with ID {} for topic {} and put schema into cache", schemaId, topic);
                schemaCache.put(topic, schema);
                schemaIdCache.put(topic, schemaId);
            }
        } catch (Exception e) {
            log.error("Error when registering schema", e);
            throw new RuntimeException(e);
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