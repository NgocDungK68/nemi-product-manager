package com.nemi.model.request.sapo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SapoWebhookRequest {
    private String storeName;
    private String accessToken;
    private String topic;
    private String address;
    private String format;
}
