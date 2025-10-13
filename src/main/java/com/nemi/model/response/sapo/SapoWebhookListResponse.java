package com.nemi.model.response.sapo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SapoWebhookListResponse {
    private List<SapoWebhookResponse.Webhook> webhooks;
}
