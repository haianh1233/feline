package com.techcat.feline.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;

public class AvroUtils {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static GenericRecord convertToGenericRecord(Object data, String avroSchemaJson) throws IOException {
        String jsonData = JSON_MAPPER.writeValueAsString(data);
        Schema schema = new Schema.Parser().parse(avroSchemaJson);

        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
        Decoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, jsonData);

        return datumReader.read(null, jsonDecoder);
    }
}
