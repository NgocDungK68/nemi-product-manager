package com.nemi.model.response.sapo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SapoWebhookResponse {
    
    @JsonProperty("webhook")
    private Webhook webhook;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Webhook {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("topic")
        private String topic;
        
        @JsonProperty("address")
        private String address;
        
        @JsonProperty("format")
        private String format;
        
        @JsonProperty("created_on")
        private String createdOn;
        
        @JsonProperty("updated_on")
        private String updatedOn;
    }
}
