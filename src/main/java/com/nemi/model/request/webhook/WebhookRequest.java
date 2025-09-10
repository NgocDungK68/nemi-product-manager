package com.nemi.model.request.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookRequest {
    private String event;
    private Long businessId;
    private String webhooksVerifyToken;
    private Object data;
}
