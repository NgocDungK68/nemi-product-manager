package com.nemi.model.response.nhanhvn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nemi.configuration.deserializer.WarrantyInventoryDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhanhvnInventoryResponse {
    private Integer id;
    private Integer typeId;
    private String code;
    private Integer remain;
    private Integer shipping;
    private Integer damaged;
    private Integer holding;
    private Integer available;

    @JsonDeserialize(using = WarrantyInventoryDeserializer.class)
    private NhanhvnProductResponse.WarrantyInventory warranty;

    private Depot depot;

    @Data
    public static class Depot {
        private Integer id;
        private Integer remain;
        private Integer shipping;
        private Integer damaged;
        private Integer holding;
        private Integer available;

        @JsonDeserialize(using = WarrantyInventoryDeserializer.class)
        private NhanhvnProductResponse.WarrantyInventory warranty;
    }
}
