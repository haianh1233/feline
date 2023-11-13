package com.techcat.feline.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationConfig {
    CONFIG_FILE_PATH("./config/config.json"),
    GATEWAY_ADMIN_API("http://localhost:8888"),
    ;

    private final String defaultValue;
}
