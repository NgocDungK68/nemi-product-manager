package com.nemi.model.request.pancake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.model.response.pancake.PancakeProductResponse;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PancakeProductWebhookRequest {

    private String id;
    @JsonProperty("note_product")
    private String noteProduct;
    private List<PancakeProductResponse.Category> categories;
    @JsonProperty("display_id")
    private String displayId;
    private List<String> images;
    @JsonProperty("inserted_at")
    private String insertedAt;
    private String name;
    private List<PancakeProductResponse.Tag> tags;
    @JsonProperty("is_published")
    private Boolean isPublished;
    @JsonProperty("manipulation_warehouses")
    private List<String> manipulationWarehouses;
    @JsonProperty("is_removed")
    private Boolean isRemoved;

    List<PancakeProductResponse.ProductData> variations;
}
