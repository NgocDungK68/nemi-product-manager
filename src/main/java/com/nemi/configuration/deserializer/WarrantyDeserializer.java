package com.nemi.configuration.deserializer;

import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

public class WarrantyDeserializer extends GenericDeserializer<NhanhvnProductResponse.Warranty> {
    public WarrantyDeserializer() {
        super(NhanhvnProductResponse.Warranty.class);
    }
}
