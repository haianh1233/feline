package com.techcat.feline.datagen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataEntry {
    @JsonProperty("_gen")
    private String gen;
    private String with;
    private String matching;
}
