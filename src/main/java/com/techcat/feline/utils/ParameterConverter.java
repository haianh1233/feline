package com.techcat.feline.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ParameterConverter {

    private static final Map<Class<?>, Function<String, ?>> conversionMap = new HashMap<>();

    static {
        conversionMap.put(int.class, Integer::parseInt);
        conversionMap.put(boolean.class, Boolean::parseBoolean);
        conversionMap.put(long.class, Long::parseLong);
        conversionMap.put(double.class, Double::parseDouble);
        conversionMap.put(String.class, String::valueOf);
    }

    public static Object convert(Class<?> targetType, String parameter) {
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, parameter);
        }

        Function<String, ?> converter = conversionMap.get(targetType);
        if (converter != null) {
            return converter.apply(parameter);
        }

        throw new UnsupportedOperationException("Unsupported parameter type: " + targetType.getName());
    }
}