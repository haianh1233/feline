package com.techcat.feline.datagen.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum GenerationStrategy {
    MATCHING("matching"),
    WITH("with"),
    ;

    private final String name;
}
