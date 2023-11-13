package com.techcat.feline.utils;

import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.Map;

public class AvroTypeDeterminer {

    private static final Map<Class<?>, Schema.Type> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put(Integer.class, Schema.Type.INT);
        TYPE_MAP.put(Long.class, Schema.Type.LONG);
        TYPE_MAP.put(Float.class, Schema.Type.FLOAT);
        TYPE_MAP.put(Double.class, Schema.Type.DOUBLE);
        TYPE_MAP.put(Boolean.class, Schema.Type.BOOLEAN);
    }

    public static Schema determineAvroType(Class<?> type) {
        return Schema.create(TYPE_MAP.getOrDefault(type, Schema.Type.STRING));
    }
}
