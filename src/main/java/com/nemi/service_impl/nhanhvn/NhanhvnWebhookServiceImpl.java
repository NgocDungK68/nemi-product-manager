package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.enums.NhanhvnEvent;
import com.nemi.constant.enums.PosName;
import com.nemi.entity.*;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.nhanhvn.NhanhvnWebhookResponse;
import com.nemi.repository.*;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnWebhookServiceImpl implements WebhookService {
    private final NhanhvnConfig nhanhvnConfig;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final NhanhvnServiceImpl nhanhvnService;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;
    private final NhanhvnClient nhanhvnClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public boolean processWebhook(String posId, HttpServletRequest request) {
        try {
            String verifyToken = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (verifyToken == null || verifyToken.isEmpty() || !verifyToken.equals(nhanhvnConfig.getVerifyToken())) {
                log.error("Invalid verify token: {}", verifyToken);
                return false;
            }

            String body = readBody(request);
            log.info("Body: {}", body);

            NhanhvnWebhookResponse webhookResponse = JsonUtils.fromJson(body, NhanhvnWebhookResponse.class);
            log.info("Webhook response convert from Body: {}", webhookResponse);
            if (webhookResponse == null || webhookResponse.getEvent() == null) {
                log.error("Invalid webhook payload: {}", body);
                return false;
            }

            return handleEvent(posId, webhookResponse);
        } catch (Exception e) {
            log.error("Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleEvent(String posId, NhanhvnWebhookResponse webhookResponse) {
        NhanhvnEvent event = NhanhvnEvent.fromValue(webhookResponse.getEvent());
        Object data = webhookResponse.getData();

        if (event == null) {
            log.warn("Unhandled webhook event: {}", webhookResponse.getEvent());
            return false;
        }

        return switch (event) {
            case WEBHOOKS_ENABLED -> handleWebhooksEnabled(posId, data);
            case PRODUCT_ADD -> handleProductAdd(posId, data);
            case PRODUCT_UPDATE -> handleProductUpdate(posId, data);
            case PRODUCT_DELETE -> handleProductDelete(posId, data);
            case ORDER_ADD -> handleOrderAdd(posId, data);
            case ORDER_UPDATE -> handleOrderUpdate(posId, data);
            case ORDER_DELETE -> handleOrderDelete(posId, data);
            default -> {
                log.warn("Unhandled event: {}", event);
                yield false;
            }
        };
    }

    private boolean handleWebhooksEnabled(String posId, Object data) {
        // logic
        return true;
    }

    /**
     * Chỉ có thể thêm được sản phẩm độc lập hoặc sản phẩm con
     */
    private boolean handleProductAdd(String posId, Object data) {
        NhanhvnProductResponse.ProductData newVariant = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("Add ProductData: {}", newVariant);

        if (newVariant == null) {
            log.error("Failed to add product data: {}", data);
            return false;
        }

        // Kiểm tra xem data có phải sản phẩm con không
        if (newVariant.getParentId() != -1) {
            Optional<ProductVariantEntity> parentOfVariantEntity = variantRepository.findById(String.valueOf(newVariant.getParentId()));
            if (parentOfVariantEntity.isPresent()) {
                // logic chuyển variant lên bảng products
                NhanhvnProductResponse.ProductData parentOfVariant = getProductById(posId, parentOfVariantEntity.get().getVariantId());
                if (parentOfVariant == null) {
                    log.warn("Failed to find parent product with id={} of new variant with id={}", newVariant.getParentId(), newVariant.getId());
                    return false;
                }

                ProductEntity parentEntity = nhanhvnService.convertToProductEntity(posId, parentOfVariant);
                productRepository.save(parentEntity);
                log.info("Converted variant with id={} to product", parentEntity.getProductId());

                variantRepository.deleteById(String.valueOf(newVariant.getParentId()));
                log.info("Deleted variant with id={} because it is now a parent product", newVariant.getParentId());
            }
        }

        ProductVariantEntity variantEntity = nhanhvnService.convertToVariantEntity(newVariant);
        variantRepository.save(variantEntity);
        log.info("Successfully add 1 variant with id={}", variantEntity.getVariantId());

        return true;
    }

    /**
     * Khi cập nhật sp con thành sp độc lập, nếu sp cha của nó không còn sp con nào thì nhanhvn sẽ gửi request với body là sp cha (lúc này là sp độc lập)
     */
    private boolean handleProductUpdate(String posId, Object data) {
        NhanhvnProductResponse.ProductData productData = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("Update ProductData: {}", productData);

        if (productData == null) {
            log.error("Failed to update product data: {}", data);
            return false;
        }

        if (productData.getParentId() == -2) {   // Trường hợp call về body sản phẩm cha
            ProductEntity productEntity = nhanhvnService.convertToProductEntity(posId, productData);
            productRepository.save(productEntity);
            log.info("Successfully update 1 product with id={}", productEntity.getProductId());
            return true;
        }

        if (productData.getParentId() == -1) {   // Trường hợp call về body sản phẩm độc lập
            // Nếu tồn tại sản phẩm cha trong DB thì sản phẩm cha lúc này là sản phẩm độc lập
            Optional<ProductEntity> productEntity = productRepository.findById(String.valueOf(productData.getId()));
            if (productEntity.isPresent()) {
                // Tìm variant có sản phẩm cha là productEntity
                List<ProductVariantEntity> variantsOfProductEntity =
                        variantRepository.findByProductId(productEntity.get().getProductId());

                if (variantsOfProductEntity.size() != 1) {
                    log.warn("Expected 1 variant but found {} for productId={}", variantsOfProductEntity.size(), productEntity.get().getProductId());
                    return false;
                }

                // Sản phẩm con => Sản phẩm độc lập
                ProductVariantEntity variantOfProductEntity = variantsOfProductEntity.get(0);
                variantOfProductEntity.setProductId("-1");
                variantRepository.save(variantOfProductEntity);
                log.info("Successfully update 1 variant with id={}", variantOfProductEntity.getVariantId());

                // Xóa sản phẩm trong product => chuyển sang sản phẩm độc lập (variant)
                productRepository.deleteById(String.valueOf(productData.getId()));
                log.info("Deleted product with id={} because it is now a variant", productEntity.get().getProductId());
            }
        } else {  // Trường hợp call về body sản phẩm con
            // Kiểm tra xem sản phẩm cha của productData có đang thuộc bảng product_variant không
            Optional<ProductVariantEntity> parentOfVariantEntity =
                    variantRepository.findById(String.valueOf(productData.getParentId()));
            if (parentOfVariantEntity.isPresent()) {
                // thêm sản phẩm cha vào bảng products
                NhanhvnProductResponse.ProductData parentProduct = getProductById(posId, String.valueOf(productData.getParentId()));
                if (parentProduct == null || parentProduct.getParentId() != -2) {
                    log.warn("Failed to find parent product with id={}", productData.getParentId());
                    return false;
                }
                ProductEntity parentProductEntity = nhanhvnService.convertToProductEntity(posId, parentProduct);
                productRepository.save(parentProductEntity);
                log.info("Converted variant with id={} to product", parentProductEntity.getProductId());

                // Xóa sản phẩm lúc này là cha ở bảng variant_product
                variantRepository.deleteById(String.valueOf(parentOfVariantEntity.get().getVariantId()));
                log.info("Deleted variant with id={} because it is now a parent product", parentOfVariantEntity.get().getVariantId());
            }
        }

        // Cập nhật sản phẩm con hoặc sản phẩm độc lập vào product_variant
        ProductVariantEntity variantEntity = nhanhvnService.convertToVariantEntity(productData);
        variantRepository.save(variantEntity);
        log.info("Successfully update 1 product variant with id={}", variantEntity.getVariantId());

        return true;
    }

    /**
     * body: {"event":"productDelete","businessId":215487,"data":["15"]}
     */
    private boolean handleProductDelete(String posId, Object data) {
        List<String> ids = objectMapper.convertValue(data, new TypeReference<>() {
        });
        if (ids.isEmpty()) {
            log.warn("No product ID provided for deletion");
            return false;
        }

        String id = ids.get(0);

        Optional<ProductVariantEntity> variantEntity = variantRepository.findById(id);
        if (variantEntity.isEmpty()) {
            log.warn("Failed to find variant with id={}", id);
            return false;
        }

        // Kiểm tra xem data có phải sản phẩm con không
        if (!variantEntity.get().getProductId().equals("-1")) {
            String parentId = variantEntity.get().getProductId();
            Optional<ProductEntity> productEntity = productRepository.findById(parentId);

            if (productEntity.isEmpty()) {
                log.warn("Failed to find parent product with id={}", parentId);
                return false;
            }

            // Kiểm tra xem sản phẩm cha có còn sản phẩm con không
            NhanhvnProductResponse.ProductData parentProduct = getProductById(posId, parentId);
            if (parentProduct == null) {
                log.warn("Failed to find parent product with id={}", parentId);
                return false;
            }

            if (parentProduct.getParentId() != -2) {
                productRepository.deleteById(parentId);
                log.info("Deleted product with id={} because it is now a variant", parentId);

                ProductVariantEntity variant = nhanhvnService.convertToVariantEntity(parentProduct);
                variantRepository.save(variant);
                log.info("Converted product with id={} to variant", variant.getVariantId());
            }

            log.info("Parent of variant with id={} is still parent product", variantEntity.get().getVariantId());
        }

        variantRepository.deleteById(id);
        log.info("Deleted variant with id={} from posId={}", id, posId);
        return true;
    }

    private boolean handleOrderAdd(String posId, Object data) {
        NhanhvnOrderResponse.OrderData orderData = objectMapper.convertValue(
                data, NhanhvnOrderResponse.OrderData.class
        );
        log.info("Add OrderData: {}", orderData);

        if (orderData == null || orderData.getInfo() == null) {
            log.error("Failed to add order data: {}", data);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = nhanhvnService.convertToOrderEntity(posId, orderData);
        if (orderEntity == null) {
            log.error("Failed to convert orderData={} to OrderEntity", orderData.getInfo().getId());
            return false;
        }

        // Save order
        orderRepository.save(orderEntity);
        log.info("Successfully saved OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Convert OrderItemEntities
        List<OrderItemEntity> orderItemEntities = nhanhvnService.convertToOrderItemEntity(orderData);
        if (orderItemEntities.isEmpty()) {
            log.warn("Order id={} has no products", orderEntity.getOrderId());
        } else {
            orderItemRepository.saveAll(orderItemEntities);
            log.info("Successfully saved {} OrderItemEntities for orderId={}",
                    orderItemEntities.size(), orderEntity.getOrderId());
        }

        return true;
    }

    private boolean handleOrderUpdate(String posId, Object data) {
        NhanhvnOrderResponse.OrderData orderData = objectMapper.convertValue(
                data, NhanhvnOrderResponse.OrderData.class
        );
        log.info("Update OrderData: {}", orderData);

        if (orderData == null || orderData.getInfo() == null) {
            log.error("Failed to update order data: {}", data);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = nhanhvnService.convertToOrderEntity(posId, orderData);
        if (orderEntity == null) {
            log.error("Failed to convert orderData={} to OrderEntity", orderData.getInfo().getId());
            return false;
        }

        // Save (insert/update)
        orderRepository.save(orderEntity);
        log.info("Successfully updated OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Sync OrderItems
        List<OrderItemEntity> orderItemEntities = nhanhvnService.convertToOrderItemEntity(orderData);
        if (orderItemEntities.isEmpty()) {
            log.warn("Order id={} has no products", orderEntity.getOrderId());
            return false;
        } else {
            // Xóa items cũ để tránh dữ liệu thừa
            orderItemRepository.deleteByOrderId(orderEntity.getOrderId());

            // Save lại items mới
            orderItemRepository.saveAll(orderItemEntities);
            log.info("Successfully synced {} OrderItemEntities for orderId={}",
                    orderItemEntities.size(), orderEntity.getOrderId());
        }

        return true;
    }


    private boolean handleOrderDelete(String posId, Object data) {
        List<String> ids = objectMapper.convertValue(data, new TypeReference<>() {
        });
        if (ids.isEmpty()) {
            log.warn("No order ID provided for deletion");
            return false;
        }

        String orderId = ids.get(0);

        Optional<OrderEntity> orderEntity = orderRepository.findById(orderId);
        if (orderEntity.isEmpty()) {
            log.warn("Failed to find order with id={}", orderId);
            return false;
        }

        // Nếu xóa cả orderItem liên quan, làm trước khi xóa order
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        if (!orderItems.isEmpty()) {
            orderItemRepository.deleteAll(orderItems);
            log.info("Deleted {} order items for orderId={}", orderItems.size(), orderId);
        }

        orderRepository.deleteById(orderId);
        log.info("Deleted order with id={} from posId={}", orderId, posId);

        return true;
    }

    private String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading request body", e);
        }
        return sb.toString();
    }

    private NhanhvnProductResponse.ProductData getProductById(String posId, String id) {
        try {
            // 1. Lấy posEntity từ DB
            PosEntity posEntity = posRepository.findById(posId)
                    .orElseThrow(() -> new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_NOTFOUND)));

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String appId = configMap.get("appId");
            String businessId = configMap.get("businessId");
            String accessToken = posEntity.getAccessToken();

            // 3. Build request
            NhanhvnRequest request = NhanhvnRequest.builder()
                    .appId(appId)
                    .businessId(businessId)
                    .accessToken(accessToken)
                    .build();

            Optional<NhanhvnProductResponse> responseOpt = nhanhvnClient.getProductById(request, id);

            if (responseOpt.isPresent() && responseOpt.get().getData() != null && !responseOpt.get().getData().isEmpty()) {
                return responseOpt.get().getData().get(0);
            }

            // Không tìm thấy sản phẩm
            return null;

        } catch (Exception e) {
            log.error("[ProductService.getProductById] Failed for posId={}, productId={}, error={}", posId, id, e.getMessage(), e);
            return null;
        }
    }
}