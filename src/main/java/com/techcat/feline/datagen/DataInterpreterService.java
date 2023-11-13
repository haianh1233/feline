package com.techcat.feline.datagen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcat.feline.datagen.model.ConfigEntry;
import com.techcat.feline.datagen.model.DataEntry;
import com.techcat.feline.utils.AvroTypeDeterminer;
import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataInterpreterService {
    private final FakerService fakerService;
    private final CacheService cacheService;
    public DataInterpreterService() {
        this.fakerService = new FakerService();
        this.cacheService = new CacheService();
    }

    public Map<String, Object> interpretConfig(ConfigEntry config) {
        Map<String, Object> data = new HashMap<>();

        // Interpretation for 'key'
        if (config.getKey() != null) {
            if ("matching".equals(config.getKey().getGen())) {
                // TODO handle complex matching
                data.put("key", cacheService.getRandomKeyData(config.getKey().getMatching().split("\\.")[0]));
            } else {
                data.put("key", fakerService.interpretDataEntry(config.getKey()));
            }
        }

        // Interpretation for 'value'
        data.put("value", interpretValue(config.getValue()));

        return data;
    }

    public void saveToCache(String topic, Map<String, Object> data) {
        cacheService.addData(topic, (String) data.get("key"));
    }


    private Object interpretValue(Map<String, Object> valueMap) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Check if it's a nested map or a DataEntry-like structure
                if (isNestedMap((Map<String, Object>) value)) {
                    // Recursively handle nested structures
                    result.put(field, interpretValue((Map<String, Object>) value));
                } else {
                    DataEntry dataEntry = new ObjectMapper().convertValue(value, DataEntry.class);
                    if ("matching".equals(dataEntry.getGen())) {
                        String[] parts = dataEntry.getMatching().split("\\.");
                        Object cachedValue = cacheService.getRandomKeyData(parts[0]);
                        result.put(field, cachedValue);
                    } else {
                        result.put(field, fakerService.interpretDataEntry(dataEntry));
                    }
                }
            }
        }

        return result;
    }

    public Schema interpretSchemaFromConfig(ConfigEntry config) {
        return generateRecordSchema(config.getValue(), config.getTopic());
    }

    private Schema generateRecordSchema(Map<String, Object> valueMap, String parentName) {
        List<Schema.Field> fields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            Schema fieldSchema;
            if (value instanceof Map) {
                // Check if the map has a DataEntry-like structure
                Map<String, Object> subMap = (Map<String, Object>) value;
                if (subMap.containsKey("_gen") && (subMap.containsKey("with") || subMap.containsKey("matching"))) {
                    // Handle the DataEntry structure
                    DataEntry dataEntry = new ObjectMapper().convertValue(value, DataEntry.class);

                    if (dataEntry.getMatching() != null) {
                        // TODO: Handle matching, for simplicity we'll just use a INTERNET_UUID => string
                        fieldSchema = AvroTypeDeterminer.determineAvroType(String.class);
                    } else {
                        fieldSchema = AvroTypeDeterminer.determineAvroType(fakerService.getDataEntryType(dataEntry.getWith()));
                    }
                } else {
                    // Recursively handle nested map structures
                    fieldSchema = generateRecordSchema(subMap, fieldName);
                }
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
            }

            fields.add(new Schema.Field(fieldName, fieldSchema, null, null));
        }

        return Schema.createRecord(parentName, null, null, false, fields);
    }

    private boolean isNestedMap(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof Map) {
                return true;
            }
        }
        return false;
    }
}
