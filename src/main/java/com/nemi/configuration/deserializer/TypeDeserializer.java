package com.nemi.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.io.IOException;

public class TypeDeserializer extends GenericDeserializer<NhanhvnProductResponse.Type> {

    public TypeDeserializer() {
        super(NhanhvnProductResponse.Type.class);
    }

    @Override
    protected NhanhvnProductResponse.Type handleNumber(JsonParser parser) throws IOException {
        NhanhvnProductResponse.Type type = new NhanhvnProductResponse.Type();
        type.setId(parser.getIntValue());
        type.setName("Unknown Name");
        return type;
    }
}

