package com.nemi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class WebhookRequest {
    private String event;
    private Long businessId;
    private String webhooksVerifyToken;
    private Object data;

}
