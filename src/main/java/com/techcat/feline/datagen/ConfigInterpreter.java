package com.techcat.feline.datagen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcat.feline.datagen.model.ConfigEntry;
import com.techcat.feline.datagen.model.ConfigEntry.DataEntry;
import com.techcat.feline.utils.AvroTypeDeterminer;
import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigInterpreter {
    private final FakerService fakerService;

    public ConfigInterpreter() {
        this.fakerService = new FakerService();
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

}
