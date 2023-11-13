package com.techcat.feline.datagen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcat.feline.datagen.model.ConfigEntry;
import com.techcat.feline.datagen.model.Data;
import com.techcat.feline.datagen.model.DataEntry;

import java.util.HashMap;
import java.util.Map;

public class DataInterpreter {
    private final FakerService fakerService;
    private final CacheService cacheService;
    public DataInterpreter() {
        this.fakerService = new FakerService();
        this.cacheService = new CacheService();
    }

    public Data interpretConfig(ConfigEntry config) {
        Data data = new Data();

        // Interpretation for 'key'
        if (config.getKey() != null) {
            if ("matching".equals(config.getKey().getGen())) {
                // TODO handle complex matching
                data.setKey(cacheService.getRandomKeyData(config.getKey().getMatching().split("\\.")[0]));
            } else {
                data.setKey(fakerService.interpretDataEntry(config.getKey()));
            }
        }

        // Interpretation for 'value'
        data.setValue(interpretValue(config.getValue()));

        return data;
    }

    public void saveToCache(String topic, Data data) {
        cacheService.addData(topic, (String) data.getKey());
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

    private boolean isNestedMap(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof Map) {
                return true;
            }
        }
        return false;
    }
}
