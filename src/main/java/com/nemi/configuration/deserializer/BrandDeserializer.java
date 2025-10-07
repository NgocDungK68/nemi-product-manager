package com.nemi.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.io.IOException;

public class BrandDeserializer extends GenericDeserializer<NhanhvnProductResponse.Brand> {
    public BrandDeserializer() {
        super(NhanhvnProductResponse.Brand.class);
    }
}