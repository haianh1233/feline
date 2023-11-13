package com.techcat.feline.datagen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigEntry {
    private String target;
    private String topic;
    private DataEntry key;
    private Map<String, Object> value;
    private Config config;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataEntry {
        @JsonProperty("_gen")
        private String gen;
        private String with;
        private String matching;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Config {
        private Throttle throttle;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Throttle {
            private int ms;
        }
    }

}
