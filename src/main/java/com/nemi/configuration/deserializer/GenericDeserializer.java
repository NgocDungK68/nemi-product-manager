package com.nemi.configuration.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Generic deserializer dùng chung cho các field có thể nhận:
 * - object: {"id": 1, "name": "NEMI"}
 * - array: [{"id": 1, "name": "NEMI"}]
 * - rỗng: []
 * - null
 * - number: 1 (chỉ có id)
 */
@Slf4j
public class GenericDeserializer<T> extends JsonDeserializer<T> {
    private final Class<T> targetClass;

    public GenericDeserializer(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        JsonToken token = parser.getCurrentToken();

        switch (token) {
            case START_ARRAY:
                parser.nextToken();
                if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                    return null;
                } else {
                    T obj = parser.readValueAs(targetClass);
                    parser.nextToken(); // bỏ qua END_ARRAY
                    return obj;
                }

            case START_OBJECT:
                return parser.readValueAs(targetClass);

            case VALUE_NUMBER_INT:
                return handleNumber(parser);

            case VALUE_NULL:
                return null;

            default:
                throw new IOException("Unexpected token for " + targetClass.getSimpleName() + ": " + token);
        }
    }

    protected T handleNumber(JsonParser parser) throws IOException {
        log.info("[GenericDeserializer.handleNumber] Token: {}, Value: {}, Location: {}",
                parser.getCurrentToken(),
                parser.getText(),
                parser.getCurrentLocation());
        return null;
    }
}
