package com.techcat.feline.datagen;

import com.github.javafaker.Faker;
import com.techcat.feline.datagen.model.DataEntry;
import com.techcat.feline.datagen.model.GenerationStrategy;
import com.techcat.feline.utils.ParameterConverter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Locale;

import static com.techcat.feline.datagen.model.GenerationStrategy.WITH;

public class FakerService {
    private final Faker faker;

    public FakerService() {
        this.faker = new Faker(Locale.US);
    }

    public Object interpretDataEntry(DataEntry entry) {
        if (entry.getGen().equals(WITH.name())) {
            String fakerExpression = entry.getWith();

            // Handle special case for current_timestamp
            if (fakerExpression.equals("#{current_timestamp}")) {
                return System.currentTimeMillis();
            }

            try {
                if (fakerExpression.startsWith("#{") && fakerExpression.endsWith("}")) {
                    // strip #{ and }
                    fakerExpression = fakerExpression.substring(2, fakerExpression.length() - 1);
                } else {
                    throw new RuntimeException("Invalid faker expression format: " + fakerExpression);
                }

                int firstDotIndex = fakerExpression.indexOf(".");
                if (firstDotIndex == -1) {
                    throw new RuntimeException("Unexpected format for faker expression: " + fakerExpression);
                }

                String utilityName = reformatUtilityName(fakerExpression.substring(0, firstDotIndex));
                String methodNameAndParams = reformatMethodName(fakerExpression.substring(firstDotIndex + 1));

                // Check if method has parameters
                boolean hasParameters = methodNameAndParams.contains("(") && methodNameAndParams.contains(")");
                String methodName = hasParameters ? methodNameAndParams.split("\\(")[0] : methodNameAndParams;
                String[] parameters = {};
                Class<?>[] parameterTypes = {};

                if (hasParameters) {
                    String rawParameters = methodNameAndParams.split("\\(")[1].split("\\)")[0];
                    parameters = rawParameters.split(",\\s*");

                    parameterTypes = new Class[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        if (parameters[i].matches("'[^']*'")) {  // Check if parameter is enclosed in single quotes
                            parameterTypes[i] = String.class;
                            parameters[i] = parameters[i].substring(1, parameters[i].length() - 1);  // Remove enclosing quotes
                        } else if (parameters[i].matches("\\d+")) {  // Check if parameter is an integer
                            parameterTypes[i] = int.class;
                        } else if (parameters[i].matches("\\d+L")) {  // Check if parameter is a long (ending with L)
                            parameterTypes[i] = long.class;
                            parameters[i] = parameters[i].substring(0, parameters[i].length() - 1);  // Remove the 'L' at the end
                        } else if (parameters[i].matches("\\d+\\.\\d+")) {  // Check if parameter is a double
                            parameterTypes[i] = double.class;
                        } else if (parameters[i].contains(".")) {  // Check if parameter references a nested type
                            String[] nestedParts = parameters[i].split("\\.");
                            if (nestedParts.length != 2) {
                                throw new RuntimeException("Unexpected nested parameter format: " + parameters[i]);
                            }
                            Class<?> nestedClass = Class.forName("com.github.javafaker." + Character.toUpperCase(utilityName.charAt(0)) + utilityName.substring(1) + "$" + nestedParts[0]);
                            parameterTypes[i] = nestedClass;
                            parameters[i] = nestedParts[1];
                        } else if (parameters[i].equalsIgnoreCase("true") || parameters[i].equalsIgnoreCase("false")) {
                            parameterTypes[i] = boolean.class;
                        } else {
                            // Handle other types or throw an error if unexpected type
                            throw new RuntimeException("Unexpected parameter type: " + parameters[i]);
                        }
                    }
                }

                Method utilityMethod = Faker.class.getMethod(utilityName);
                Object utilityInstance = utilityMethod.invoke(faker);

                Method method;
                Object[] paramValues;

                if (hasParameters) {
                    paramValues = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        paramValues[i] = ParameterConverter.convert(parameterTypes[i], parameters[i]);
                    }

                    method = utilityInstance.getClass().getMethod(reformatMethodName(methodName), parameterTypes);
                    return method.invoke(utilityInstance, paramValues);
                } else {
                    method = utilityInstance.getClass().getMethod(reformatMethodName(methodName));
                    return method.invoke(utilityInstance);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to interpret faker expression: " + fakerExpression, e);
            }
        }

        throw new RuntimeException("Failed to interpret faker data entry: " + entry);
    }

    @NotNull
    private String reformatUtilityName(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    @NotNull
    private String reformatMethodName(String s) {
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            camelCaseString.append(Character.toUpperCase(parts[i].charAt(0)));
            camelCaseString.append(parts[i].substring(1));
        }

        return camelCaseString.toString();
    }

    // TODO improve the performance of this method
    // We could use a map to cache the results of this method
    public Class<?> getDataEntryType(String withValue) {
        // Hacky way to determine the type of the faker method
        DataEntry dataEntry = new DataEntry();
        dataEntry.setGen(WITH.name());
        dataEntry.setWith(withValue);

        return interpretDataEntry(dataEntry).getClass();
    }
}
