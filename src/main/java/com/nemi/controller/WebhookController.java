package com.nemi.controller;

import com.nemi.model.request.WebhookRequest;
import com.nemi.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);


    // Thay YOUR_VERIFY_TOKEN bằng token bạn đặt trong app Nhanh.vn
    private static final String VERIFY_TOKEN = "YOUR_VERIFY_TOKEN";

    @PostMapping("/nhanh")
    public ResponseEntity<String> receiveWebhook(@RequestBody WebhookRequest webhookRequest) {

        logger.info("Received webhook - Event: {}, BusinessId: {}",
                webhookRequest.getEvent(), webhookRequest.getBusinessId());

        // Verify token
        if (!VERIFY_TOKEN.equals(webhookRequest.getWebhooksVerifyToken())) {
            logger.warn("Invalid webhook token");
            return ResponseEntity.status(401).body("Invalid token");
        }

        // Log data để debug
        logger.info("Webhook data: {}", webhookRequest.getData());

        // TODO: Xử lý logic của bạn ở đây
        processWebhook(webhookRequest);

        return ResponseEntity.ok("OK");
    }

    private void processWebhook(WebhookRequest request) {
        // Xử lý đơn giản theo event type
        String eventType = request.getEvent();

        switch (eventType) {
            case "webhooksEnabled":
                logger.info("Webhooks đã được bật");
                break;
            case "orderAdd":
                logger.info("Có đơn hàng mới");
                break;
            case "orderUpdate":
                logger.info("Đơn hàng được cập nhật");
                break;
            case "productAdd":
                logger.info("Có sản phẩm mới");
                break;
            case "inventoryChange":
                logger.info("Tồn kho thay đổi");
                break;
            default:
                logger.info("Event khác: {}", eventType);
        }
    }
}
