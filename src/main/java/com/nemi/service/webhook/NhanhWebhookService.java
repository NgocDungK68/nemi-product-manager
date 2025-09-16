package com.nemi.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.request.webhook.OrderWebhook;
import com.nemi.model.request.webhook.WebhookRequest;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class NhanhWebhookService implements WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(NhanhWebhookService.class);

    @Autowired
    private ObjectMapper objectMapper;
    private static final String VERIFY_TOKEN = "nemiWebhook123!@#";

    @Override
    public String getWebhookType() {
        return "nhanh";
    }

    @Override
    public boolean supports(String webhookType) {
        return "nhanh".equalsIgnoreCase(webhookType);
    }

    @Override
    public void processWebhook(HttpServletRequest request) {
        logger.info("=== NHANH.VN WEBHOOK PROCESSING START ===");
        
        // Log headers for debugging
        Map<String, String> headers = extractHeaders(request);
        logger.info("=== WEBHOOK HEADERS ===");
        headers.forEach((k,v)-> logger.info("Header: {} = {}", k, v));

        // Read raw body
        String body = readBody(request);
        logger.info("Raw payload body: {}", body);

        // Parse JSON
        JsonNode root = null;
        try {
            root = objectMapper.readTree(body);
            logger.info("JSON parsing successful");
        } catch (IOException e) {
            logger.error("Error parsing JSON body", e);
            throw new RuntimeException("Invalid JSON payload", e);
        }

        if (root != null) {
            // Extract and validate basic fields
            String event = extractField(root, "event");
            String businessId = extractField(root, "businessId");
            String token = extractField(root, "webhooksVerifyToken");
            
            logger.info("=== WEBHOOK EVENT DETAILS ===");
            logger.info("Event type: {}", event);
            logger.info("Business ID: {}", businessId);
            logger.info("Verify token: {}", token);
            
            // Debug webhook configuration
            debugWebhookConfiguration(root);
            
            // Validate token if present
            if (token != null && !VERIFY_TOKEN.equals(token)) {
                logger.error("Invalid webhook token. Expected: {}, Received: {}", VERIFY_TOKEN, token);
                throw new RuntimeException("Invalid webhook verification token");
            }
            
            // Process based on event type
            if (event != null) {
                processEventByType(event, root);
            } else {
                logger.warn("No event type specified in webhook payload");
                // Try to extract alternative event field names
                String alternativeEvent = extractAlternativeEventField(root);
                if (alternativeEvent != null) {
                    logger.info("Found alternative event field: {}", alternativeEvent);
                    processEventByType(alternativeEvent, root);
                }
            }
            
            // Log complete JSON for debugging
            logger.info("=== FULL JSON PAYLOAD ===");
            logger.info("{}", root.toPrettyString());
        }
        
        logger.info("=== NHANH.VN WEBHOOK PROCESSING COMPLETE ===");
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getHeader(name);
            map.put(name, value);
        }
        return map;
    }

    private String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("Error reading request body", e);
        }
        return sb.toString();
    }

    /**
     * Process webhook based on event type
     */
    private void processEventByType(String eventType, JsonNode payload) {
        logger.info("Processing event type: {}", eventType);
        
        switch (eventType.toLowerCase()) {
            case "addorder":
            case "orderadd":
                processAddOrderEvent(payload);
                break;
            case "updateorder":
            case "orderupdate":
                processUpdateOrderEvent(payload);
                break;
            case "addproduct":
            case "productadd":
                processAddProductEvent(payload);
                break;
            case "updateproduct":
            case "productupdate":
                processUpdateProductEvent(payload);
                break;
            case "addcustomer":
            case "customeradd":
                processAddCustomerEvent(payload);
                break;
            case "updatecustomer":
            case "customerupdate":
                processUpdateCustomerEvent(payload);
                break;
            case "webhooksenabled":
                processWebhooksEnabledEvent(payload);
                break;
            case "addcategory":
            case "categoryadd":
                processAddCategoryEvent(payload);
                break;
            case "updatecategory":
            case "categoryupdate":
                processUpdateCategoryEvent(payload);
                break;
            default:
                logger.warn("Unknown event type: {}. Processing as generic event.", eventType);
                processGenericEvent(eventType, payload);
                break;
        }
    }

    /**
     * Process addOrder event
     */
    private void processAddOrderEvent(JsonNode payload) {
        logger.info("=== PROCESSING ADD ORDER EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                // Try to parse as OrderWebhook
                OrderWebhook orderWebhook = objectMapper.treeToValue(data, OrderWebhook.class);
                
                logger.info("Order ID: {}", orderWebhook.getId());
                logger.info("Customer: {} ({})", orderWebhook.getCustomerName(), orderWebhook.getCustomerMobile());
                logger.info("Total Price: {}", orderWebhook.getTotalPrice());
                logger.info("Status: {} ({})", orderWebhook.getStatus(), orderWebhook.getStatusText());
                logger.info("Created: {}", orderWebhook.getCreatedDateTime());
                
                // Log products
                if (orderWebhook.getProducts() != null && !orderWebhook.getProducts().isEmpty()) {
                    logger.info("Products ({} items):", orderWebhook.getProducts().size());
                    orderWebhook.getProducts().forEach(product -> 
                        logger.info("  - Product ID: {}, Name: {}, Qty: {}, Price: {}", 
                            product.getProductId(), product.getProductName(), 
                            product.getQuantity(), product.getPrice())
                    );
                }
                
                // Log payments
                if (orderWebhook.getPayments() != null && !orderWebhook.getPayments().isEmpty()) {
                    logger.info("Payments ({} methods):", orderWebhook.getPayments().size());
                    orderWebhook.getPayments().forEach(payment -> 
                        logger.info("  - Method: {}, Amount: {}", payment.getMethod(), payment.getAmount())
                    );
                }
                
                // TODO: Add business logic here (save to database, send notifications, etc.)
                logger.info("Order webhook processed successfully");
                
            } else {
                logger.warn("No data field found in addOrder event");
            }
        } catch (Exception e) {
            logger.error("Error processing addOrder event", e);
        }
    }

    /**
     * Process updateOrder event  
     */
    private void processUpdateOrderEvent(JsonNode payload) {
        logger.info("=== PROCESSING UPDATE ORDER EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                // Similar processing as addOrder but for updates
                OrderWebhook orderWebhook = objectMapper.treeToValue(data, OrderWebhook.class);
                
                logger.info("Updated Order ID: {}", orderWebhook.getId());
                logger.info("New Status: {} ({})", orderWebhook.getStatus(), orderWebhook.getStatusText());
                logger.info("Updated: {}", orderWebhook.getUpdatedDateTime());
                
                // TODO: Add update-specific business logic
                logger.info("Order update webhook processed successfully");
                
            } else {
                logger.warn("No data field found in updateOrder event");
            }
        } catch (Exception e) {
            logger.error("Error processing updateOrder event", e);
        }
    }

    /**
     * Process addProduct event
     */
    private void processAddProductEvent(JsonNode payload) {
        logger.info("=== PROCESSING ADD PRODUCT EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Product data: {}", data.toPrettyString());
                
                // Extract common product fields
                logProductDetails(data);
                
                // TODO: Add product processing business logic
                logger.info("Product add webhook processed successfully");
                
            } else {
                logger.warn("No data field found in addProduct event");
            }
        } catch (Exception e) {
            logger.error("Error processing addProduct event", e);
        }
    }

    /**
     * Process updateProduct event
     */
    private void processUpdateProductEvent(JsonNode payload) {
        logger.info("=== PROCESSING UPDATE PRODUCT EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Updated product data: {}", data.toPrettyString());
                
                logProductDetails(data);
                
                // TODO: Add product update business logic
                logger.info("Product update webhook processed successfully");
                
            } else {
                logger.warn("No data field found in updateProduct event");
            }
        } catch (Exception e) {
            logger.error("Error processing updateProduct event", e);
        }
    }

    /**
     * Process addCustomer event
     */
    private void processAddCustomerEvent(JsonNode payload) {
        logger.info("=== PROCESSING ADD CUSTOMER EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Customer data: {}", data.toPrettyString());
                
                logCustomerDetails(data);
                
                // TODO: Add customer processing business logic
                logger.info("Customer add webhook processed successfully");
                
            } else {
                logger.warn("No data field found in addCustomer event");
            }
        } catch (Exception e) {
            logger.error("Error processing addCustomer event", e);
        }
    }

    /**
     * Process updateCustomer event
     */
    private void processUpdateCustomerEvent(JsonNode payload) {
        logger.info("=== PROCESSING UPDATE CUSTOMER EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Updated customer data: {}", data.toPrettyString());
                
                logCustomerDetails(data);
                
                // TODO: Add customer update business logic
                logger.info("Customer update webhook processed successfully");
                
            } else {
                logger.warn("No data field found in updateCustomer event");
            }
        } catch (Exception e) {
            logger.error("Error processing updateCustomer event", e);
        }
    }

    /**
     * Process webhooksEnabled event
     */
    private void processWebhooksEnabledEvent(JsonNode payload) {
        logger.info("=== PROCESSING WEBHOOKS ENABLED EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Webhooks enabled data: {}", data.toPrettyString());
                
                // Log enabled webhook types
                if (data.has("enabledEvents")) {
                    JsonNode enabledEvents = data.get("enabledEvents");
                    logger.info("Enabled webhook events: {}", enabledEvents.toString());
                }
                
                // TODO: Add webhooks enabled business logic
                logger.info("Webhooks enabled event processed successfully");
                
            } else {
                logger.warn("No data field found in webhooksEnabled event");
            }
        } catch (Exception e) {
            logger.error("Error processing webhooksEnabled event", e);
        }
    }

    /**
     * Process addCategory event
     */
    private void processAddCategoryEvent(JsonNode payload) {
        logger.info("=== PROCESSING ADD CATEGORY EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Category data: {}", data.toPrettyString());
                
                logCategoryDetails(data);
                
                // TODO: Add category processing business logic
                logger.info("Category add webhook processed successfully");
                
            } else {
                logger.warn("No data field found in addCategory event");
            }
        } catch (Exception e) {
            logger.error("Error processing addCategory event", e);
        }
    }

    /**
     * Process updateCategory event
     */
    private void processUpdateCategoryEvent(JsonNode payload) {
        logger.info("=== PROCESSING UPDATE CATEGORY EVENT ===");
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Updated category data: {}", data.toPrettyString());
                
                logCategoryDetails(data);
                
                // TODO: Add category update business logic
                logger.info("Category update webhook processed successfully");
                
            } else {
                logger.warn("No data field found in updateCategory event");
            }
        } catch (Exception e) {
            logger.error("Error processing updateCategory event", e);
        }
    }

    /**
     * Process unknown/generic events
     */
    private void processGenericEvent(String eventType, JsonNode payload) {
        logger.info("=== PROCESSING GENERIC EVENT: {} ===", eventType);
        
        try {
            JsonNode data = payload.get("data");
            if (data != null) {
                logger.info("Generic event data: {}", data.toPrettyString());
                
                // TODO: Add generic event processing logic
                logger.info("Generic event processed successfully");
                
            } else {
                logger.warn("No data field found in {} event", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing generic event: {}", eventType, e);
        }
    }

    /**
     * Helper method to extract field from JSON node
     */
    private String extractField(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    /**
     * Debug webhook configuration to help troubleshoot missing events
     */
    private void debugWebhookConfiguration(JsonNode payload) {
        logger.info("=== WEBHOOK CONFIGURATION DEBUG ===");
        
        // Log all root level fields
        payload.fieldNames().forEachRemaining(fieldName -> {
            JsonNode fieldValue = payload.get(fieldName);
            if (fieldValue.isValueNode()) {
                logger.info("Root field: {} = {}", fieldName, fieldValue.asText());
            } else {
                logger.info("Root field: {} = [{}]", fieldName, fieldValue.getNodeType());
            }
        });
        
        // Check for webhook configuration in data field
        JsonNode data = payload.get("data");
        if (data != null && data.has("webhookConfig")) {
            JsonNode webhookConfig = data.get("webhookConfig");
            logger.info("Webhook config found: {}", webhookConfig.toPrettyString());
        }
        
        // Check for enabled events
        if (data != null && data.has("enabledEvents")) {
            JsonNode enabledEvents = data.get("enabledEvents");
            logger.info("Enabled events: {}", enabledEvents.toString());
        }
        
        // Log timestamp for webhook debugging
        logger.info("Webhook received at: {}", java.time.LocalDateTime.now());
        logger.info("Webhook URL endpoint: /webhook/nhanh");
        logger.info("Expected events: addOrder/orderAdd, updateOrder/orderUpdate, addProduct/productAdd, updateProduct/productUpdate, addCustomer/customerAdd, updateCustomer/customerUpdate");
    }

    /**
     * Try to extract event from alternative field names
     */
    private String extractAlternativeEventField(JsonNode payload) {
        // Common alternative field names for event type
        String[] alternativeFields = {"type", "eventType", "action", "webhook_event", "topic"};
        
        for (String fieldName : alternativeFields) {
            String value = extractField(payload, fieldName);
            if (value != null) {
                logger.info("Found event in alternative field '{}': {}", fieldName, value);
                return value;
            }
        }
        
        // Check in data field
        JsonNode data = payload.get("data");
        if (data != null) {
            for (String fieldName : alternativeFields) {
                String value = extractField(data, fieldName);
                if (value != null) {
                    logger.info("Found event in data.{}: {}", fieldName, value);
                    return value;
                }
            }
        }
        
        return null;
    }

    /**
     * Helper method to log product details
     */
    private void logProductDetails(JsonNode productData) {
        String productId = extractField(productData, "id");
        String productName = extractField(productData, "name");
        String productCode = extractField(productData, "code");
        String price = extractField(productData, "price");
        String status = extractField(productData, "status");
        
        logger.info("Product ID: {}", productId);
        logger.info("Product Name: {}", productName);
        logger.info("Product Code: {}", productCode);
        logger.info("Price: {}", price);
        logger.info("Status: {}", status);
    }

    /**
     * Helper method to log customer details
     */
    private void logCustomerDetails(JsonNode customerData) {
        String customerId = extractField(customerData, "id");
        String customerName = extractField(customerData, "name");
        String mobile = extractField(customerData, "mobile");
        String email = extractField(customerData, "email");
        String address = extractField(customerData, "address");
        
        logger.info("Customer ID: {}", customerId);
        logger.info("Customer Name: {}", customerName);
        logger.info("Mobile: {}", mobile);
        logger.info("Email: {}", email);
        logger.info("Address: {}", address);
    }

    /**
     * Helper method to log category details
     */
    private void logCategoryDetails(JsonNode categoryData) {
        String categoryId = extractField(categoryData, "id");
        String categoryName = extractField(categoryData, "name");
        String parentId = extractField(categoryData, "parentId");
        String status = extractField(categoryData, "status");
        
        logger.info("Category ID: {}", categoryId);
        logger.info("Category Name: {}", categoryName);
        logger.info("Parent ID: {}", parentId);
        logger.info("Status: {}", status);
    }
}