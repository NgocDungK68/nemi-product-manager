package com.nemi.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.io.IOException;

public class BrandDeserializer extends JsonDeserializer<NhanhvnProductResponse.Brand> {

    @Override
    public NhanhvnProductResponse.Brand deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        switch (token) {
            case START_ARRAY:
                // Trường hợp array: [] hoặc [{"id":1,"name":"NEMI"}]
                parser.nextToken(); // Di chuyển đến token tiếp theo

                if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                    // Mảng rỗng: "brand": []
                    return null;
                } else {
                    // Mảng có object: "brand": [{"id":1,"name":"NEMI"}]
                    NhanhvnProductResponse.Brand brand = parser.readValueAs(NhanhvnProductResponse.Brand.class);
                    parser.nextToken(); // Skip END_ARRAY
                    return brand;
                }

            case START_OBJECT:
                // Trường hợp object đơn: "brand": {"id":1,"name":"NEMI"}
                return parser.readValueAs(NhanhvnProductResponse.Brand.class);

            case VALUE_NULL:
                // Trường hợp null
                return null;

            default:
                throw new IOException("Unexpected token for Brand field: " + token);
        }
    }
}