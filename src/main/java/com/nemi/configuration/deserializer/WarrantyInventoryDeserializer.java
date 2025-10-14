package com.nemi.configuration.deserializer;

import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

public class WarrantyInventoryDeserializer extends GenericDeserializer<NhanhvnProductResponse.WarrantyInventory> {
    public WarrantyInventoryDeserializer() {
        super(NhanhvnProductResponse.WarrantyInventory.class);
    }
}
