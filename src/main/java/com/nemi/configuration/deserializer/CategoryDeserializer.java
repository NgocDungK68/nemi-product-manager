package com.nemi.configuration.deserializer;

import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

public class CategoryDeserializer extends GenericDeserializer<NhanhvnProductResponse.Category> {
    public CategoryDeserializer() {
        super(NhanhvnProductResponse.Category.class);
    }
}
