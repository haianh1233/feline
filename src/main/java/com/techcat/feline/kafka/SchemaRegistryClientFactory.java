package com.techcat.feline.kafka;

import com.techcat.feline.config.KafkaEnvConfigLoader;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class SchemaRegistryClientFactory {
    private final KafkaEnvConfigLoader kafkaEnvConfigLoader;
    private static final int CACHE_SIZE = 100;


    private SchemaRegistryClientFactory(KafkaEnvConfigLoader kafkaEnvConfigLoader) {
        this.kafkaEnvConfigLoader = kafkaEnvConfigLoader;
    }

    private static class SingletonHelper {
        private static final SchemaRegistryClient INSTANCE = new SchemaRegistryClientFactory(new KafkaEnvConfigLoader()).createSchemaRegistryClient();
    }

    public static SchemaRegistryClient schemaRegistryClient() {
        return SingletonHelper.INSTANCE;
    }

    private SchemaRegistryClient createSchemaRegistryClient() {
        Properties kafkaProperties = kafkaEnvConfigLoader.loadPropertiesFromEnvironment();
        String schemaRegistryUrl = kafkaProperties.getProperty("schema.registry.url");

        validateSchemaRegistryUrl(schemaRegistryUrl);

        return new CachedSchemaRegistryClient(schemaRegistryUrl, CACHE_SIZE);
    }

    private static void validateSchemaRegistryUrl(String schemaRegistryUrl) {
        if (schemaRegistryUrl == null) {
            throw new RuntimeException("Missing 'schema.registry.url' in properties.");
        }
    }
}
