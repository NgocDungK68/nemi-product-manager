package com.nemi.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.io.IOException;

public class TypeDeserializer extends JsonDeserializer<NhanhvnProductResponse.Type> {
    @Override
    public NhanhvnProductResponse.Type deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        switch (token) {
            case START_ARRAY:
                parser.nextToken();

                if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                    return null;
                } else {
                    NhanhvnProductResponse.Type type = parser.readValueAs(NhanhvnProductResponse.Type.class);
                    parser.nextToken(); // Skip END_ARRAY
                    return type;
                }
            case START_OBJECT:
                return parser.readValueAs(NhanhvnProductResponse.Type.class);
            case VALUE_NUMBER_INT:
                NhanhvnProductResponse.Type type = new NhanhvnProductResponse.Type();
                type.setId(parser.getIntValue());
                type.setName(null);
                return type;
            case VALUE_NULL:
                return null;
            default:
                throw new IOException("Unexpected token for Type field: " + token);
        }
    }
}
