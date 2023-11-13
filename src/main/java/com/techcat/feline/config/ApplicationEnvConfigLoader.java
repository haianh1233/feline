package com.techcat.feline.config;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ApplicationEnvConfigLoader {

    public String getConfig(ApplicationConfig applicationConfig) {
        return System.getenv().getOrDefault(applicationConfig.toString(), applicationConfig.getDefaultValue());
    }
}
