package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.constant.WebhookConstants;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductId;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.VariantId;
import com.nemi.entity.WebhookHistoryEntity;
import com.nemi.enums.NhanhvnEvent;
import com.nemi.enums.PosName;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.mapper.NhanhvnMapper;
import com.nemi.model.request.nhanhvn.NhanhvnOrderWebhookRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.request.nhanhvn.NhanhvnWebhookRequest;
import com.nemi.model.response.nhanhvn.NhanhvnInventoryResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.WebhookHistoryRepository;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnWebhookServiceImpl implements WebhookService {
    private final NhanhvnConfig nhanhvnConfig;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;
    private final NhanhvnClient nhanhvnClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WebhookHistoryRepository webhookHistoryRepository;
    private final NhanhvnMapper nhanhvnMapper;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    @Transactional
    @PreAuthorize("@nhanhvnAuth.checkWebhookToken(#headers, #posId)")
    public boolean processWebhook(String posId, String posName, Map<String, String> headers, Object body) {
        WebhookHistoryEntity webhookHistory = WebhookHistoryEntity.builder()
                .header(JsonUtils.toJson(headers))
                .status(WebhookConstants.Status.FAILED)
                .posId(posId)
                .posName(posName)
                .build();
        try {

            NhanhvnWebhookRequest webhookRequest = JsonUtils.map(body, NhanhvnWebhookRequest.class);
            webhookHistory.setBody(JsonUtils.toJson(webhookRequest));

            log.info("[NhanhvnWebhookServiceImpl.processWebhook] Webhook response convert from Body: {}", webhookRequest);
            if (ObjectUtils.isEmpty(webhookRequest) || ObjectUtils.isEmpty(webhookRequest.getEvent())) {
                log.error("[NhanhvnWebhookServiceImpl.processWebhook] Invalid webhook payload: {}", body);
                return false;
            }

            boolean isSuccess = handleEvent(posId, webhookRequest, webhookHistory);
            String webhookStatus = isSuccess ? WebhookConstants.Status.SUCCESS : WebhookConstants.Status.FAILED;
            webhookHistory.setStatus(webhookStatus);
            webhookHistory.setCreatedBy(WebhookConstants.WEBHOOK);
            webhookHistory.setUpdatedBy(WebhookConstants.WEBHOOK);
            return isSuccess;
        } catch (Exception e) {
            log.error("[NhanhvnWebhookServiceImpl.processWebhook] Process webhook failed: {}", e.getMessage(), e);
            return false;
        } finally {
            webhookHistoryRepository.save(webhookHistory);
        }
    }

    private boolean handleEvent(String posId, NhanhvnWebhookRequest webhookRequest, WebhookHistoryEntity webhookHistory) {
        NhanhvnEvent event = NhanhvnEvent.fromValue(webhookRequest.getEvent());
        Object data = webhookRequest.getData();

        if (ObjectUtils.isEmpty(event)) {
            log.warn("[NhanhvnWebhookServiceImpl.handleEvent] Unhandled webhook event: {}", webhookRequest.getEvent());
            return false;
        }

        webhookHistory.setSyncType(event.getSyncType());
        webhookHistory.setEventType(event.getEventType());

        return switch (event) {
            case WEBHOOKS_ENABLED -> handleWebhooksEnabled(posId, data);
            case PRODUCT_ADD -> handleProductAdd(posId, data);
            case PRODUCT_UPDATE -> handleProductUpdate(posId, data);
            case PRODUCT_DELETE -> handleProductDelete(posId, data);
            case ORDER_ADD -> handleOrderAdd(posId, data);
            case ORDER_UPDATE -> handleOrderUpdate(posId, data);
            case ORDER_DELETE -> handleOrderDelete(posId, data);
            case INVENTORY_CHANGE -> handleInventoryChange(posId, data);
            default -> {
                log.warn("[NhanhvnWebhookServiceImpl.handleEvent] Unhandled event: {}", event);
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
        log.info("[NhanhvnWebhookServiceImpl.handleProductAdd] Add ProductData: {}", newVariant);

        if (ObjectUtils.isEmpty(newVariant)) {
            log.error("[NhanhvnWebhookServiceImpl.handleProductAdd] Failed to add product data: {}", data);
            return false;
        }

        // Kiểm tra xem data có phải sản phẩm con không
        if (!(newVariant.getParentId()).equals(NhanhvnConstants.STANDALONE_PRODUCT)) {
            VariantId variantId = new VariantId(String.valueOf(newVariant.getParentId()), posId);
            Optional<ProductVariantEntity> parentOfVariantEntity = variantRepository.findById(variantId);

            if (parentOfVariantEntity.isPresent()) {
                // logic chuyển variant lên bảng products
                NhanhvnProductResponse.ProductData parentOfVariant = getProductById(posId, parentOfVariantEntity.get().getVariantId());
                if (ObjectUtils.isEmpty(parentOfVariant)) {
                    log.warn("[NhanhvnWebhookServiceImpl.handleProductAdd] Failed to find parent product with id={} of new variant with id={}", newVariant.getParentId(), newVariant.getId());
                    return false;
                }

                ProductEntity parentEntity = nhanhvnMapper.convertToProductEntity(posId, parentOfVariant, PosName.WEBHOOK.getValue(), null);
                parentEntity.setCreatedBy(parentOfVariantEntity.get().getCreatedBy());
                productRepository.save(parentEntity);
                log.info("[NhanhvnWebhookServiceImpl.handleProductAdd] Converted variant with id={} to product", parentEntity.getProductId());

                variantRepository.deleteById(variantId);
                log.info("[NhanhvnWebhookServiceImpl.handleProductAdd] Deleted variant with id={} because it is now a parent product", newVariant.getParentId());
            }
        }

        ProductVariantEntity variantEntity = nhanhvnMapper.convertToVariantEntity(posId, newVariant, PosName.WEBHOOK.getValue());
        variantRepository.save(variantEntity);
        log.info("[NhanhvnWebhookServiceImpl.handleProductAdd] Successfully add 1 variant with id={}", variantEntity.getVariantId());

        return true;
    }

    /**
     * Khi cập nhật sp con thành sp độc lập, nếu sp cha của nó không còn sp con nào thì nhanhvn sẽ gửi request với body là sp cha (lúc này là sp độc lập)
     */
    private boolean handleProductUpdate(String posId, Object data) {
        NhanhvnProductResponse.ProductData productData = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Update ProductData: {}", productData);

        if (ObjectUtils.isEmpty(productData)) {
            log.error("[NhanhvnWebhookServiceImpl.handleProductUpdate] Failed to update product data: {}", data);
            return false;
        }

        if ((productData.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) {   // Trường hợp call về body sản phẩm cha
            ProductEntity productEntity = nhanhvnMapper.convertToProductEntity(posId, productData, PosName.WEBHOOK.getValue(), null);
            productRepository.save(productEntity);
            log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Successfully update 1 product with id={}", productEntity.getProductId());
            return true;
        }

        if ((productData.getParentId()).equals(NhanhvnConstants.STANDALONE_PRODUCT)) {   // Trường hợp call về body sản phẩm độc lập
            // Nếu tồn tại sản phẩm cha trong DB thì sản phẩm cha lúc này là sản phẩm độc lập
            Optional<ProductEntity> productEntity = productRepository.findById(
                    new ProductId(String.valueOf(productData.getId()), posId)
            );
            if (productEntity.isPresent()) {
                // Tìm variant có sản phẩm cha là productEntity
                List<ProductVariantEntity> variantsOfProductEntity =
                        variantRepository.findByProductId(productEntity.get().getProductId());

                if (!Objects.equals(variantsOfProductEntity.size(), 1)) {
                    log.warn("[NhanhvnWebhookServiceImpl.handleProductUpdate] Expected 1 variant but found {} for productId={}", variantsOfProductEntity.size(), productEntity.get().getProductId());
                    return false;
                }

                // Sản phẩm con => Sản phẩm độc lập
                ProductVariantEntity variantOfProductEntity = variantsOfProductEntity.get(0);
                variantOfProductEntity.setProductId(String.valueOf(NhanhvnConstants.STANDALONE_PRODUCT));
                variantRepository.save(variantOfProductEntity);
                log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Successfully update 1 variant with id={}", variantOfProductEntity.getVariantId());

                // Xóa sản phẩm trong product => chuyển sang sản phẩm độc lập (variant)
                productRepository.deleteById(
                        new ProductId(String.valueOf(productData.getId()), posId)
                );
                log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Deleted product with id={} because it is now a variant", productEntity.get().getProductId());
            }
        } else {  // Trường hợp call về body sản phẩm con
            // Kiểm tra xem sản phẩm cha của productData có đang thuộc bảng product_variant không
            Optional<ProductVariantEntity> parentOfVariantEntity =
                    variantRepository.findById(new VariantId(String.valueOf(productData.getParentId()), posId));
            if (parentOfVariantEntity.isPresent()) {
                // thêm sản phẩm cha vào bảng products
                NhanhvnProductResponse.ProductData parentProduct = getProductById(posId, String.valueOf(productData.getParentId()));
                if (ObjectUtils.isEmpty(parentProduct) || !(parentProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) {
                    log.warn("[NhanhvnWebhookServiceImpl.handleProductUpdate] Failed to find parent product with id={}", productData.getParentId());
                    return false;
                }

                ProductEntity parentProductEntity = nhanhvnMapper.convertToProductEntity(posId, parentProduct, PosName.WEBHOOK.getValue(), null);
                parentProductEntity.setCreatedBy(parentOfVariantEntity.get().getCreatedBy());
                productRepository.save(parentProductEntity);
                log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Converted variant with id={} to product", parentProductEntity.getProductId());

                // Xóa sản phẩm lúc này là cha ở bảng variant_product
                variantRepository.deleteById(new VariantId(String.valueOf(parentOfVariantEntity.get().getVariantId()), posId));

                log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Deleted variant with id={} because it is now a parent product", parentOfVariantEntity.get().getVariantId());
            }
        }

        // Cập nhật sản phẩm con hoặc sản phẩm độc lập vào product_variant
        ProductVariantEntity variantEntity = nhanhvnMapper.convertToVariantEntity(posId, productData, PosName.WEBHOOK.getValue());
        variantRepository.save(variantEntity);
        log.info("[NhanhvnWebhookServiceImpl.handleProductUpdate] Successfully update 1 product variant with id={}", variantEntity.getVariantId());

        return true;
    }

    /**
     * body: {"event":"productDelete","businessId":215487,"data":["15"]}
     */
    private boolean handleProductDelete(String posId, Object data) {
        List<String> ids = objectMapper.convertValue(data, new TypeReference<>() {
        });
        if (ids.isEmpty()) {
            log.warn("[NhanhvnWebhookServiceImpl.handleProductDelete] No product ID provided for deletion");
            return false;
        }

        for (String id : ids) {
            VariantId variantId = new VariantId(id, posId);
            Optional<ProductVariantEntity> variantEntity = variantRepository.findById(variantId);
            if (variantEntity.isEmpty()) {
                log.warn("[NhanhvnWebhookServiceImpl.handleProductDelete] Failed to find variant with id={}", id);
                return false;
            }

            // Kiểm tra xem data có phải sản phẩm con không
            if (!variantEntity.get().getProductId().equals(String.valueOf(NhanhvnConstants.STANDALONE_PRODUCT))) {
                String parentId = variantEntity.get().getProductId();
                Optional<ProductEntity> productEntity = productRepository.findById(
                        new ProductId(parentId, posId)
                );

                if (productEntity.isEmpty()) {
                    log.warn("[NhanhvnWebhookServiceImpl.handleProductDelete] Failed to find parent product with id={} in database", parentId);
                    return false;
                }

                // Kiểm tra xem sản phẩm cha có còn sản phẩm con không
                NhanhvnProductResponse.ProductData parentProduct = getProductById(posId, parentId);
                if (ObjectUtils.isEmpty(parentProduct)) {
                    log.warn("[NhanhvnWebhookServiceImpl.handleProductDelete] Failed to find parent product with id={}", parentId);
                    return false;
                }

                if (!(parentProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) {
                    productRepository.deleteById(new ProductId(parentId, posId));
                    log.info("[NhanhvnWebhookServiceImpl.handleProductDelete] Deleted product with id={} because it is now a variant", parentId);

                    ProductVariantEntity variant = nhanhvnMapper.convertToVariantEntity(posId, parentProduct, PosName.WEBHOOK.getValue());
                    variant.setCreatedBy(productEntity.get().getCreatedBy());
                    variantRepository.save(variant);
                    log.info("[NhanhvnWebhookServiceImpl.handleProductDelete] Converted product with id={} to variant", variant.getVariantId());
                }

                log.info("[NhanhvnWebhookServiceImpl.handleProductDelete] Parent of variant with id={} is still parent product", variantEntity.get().getVariantId());
            }

            variantRepository.deleteById(variantId);
            log.info("[NhanhvnWebhookServiceImpl.handleProductDelete] Deleted variant with id={} from posId={}", id, posId);
        }

        return true;
    }

    private boolean handleOrderAdd(String posId, Object data) {
        NhanhvnOrderWebhookRequest orderData = objectMapper.convertValue(
                data, NhanhvnOrderWebhookRequest.class
        );
        log.info("[NhanhvnWebhookServiceImpl.handleOrderAdd] Add OrderData: {}", orderData);

        if (ObjectUtils.isEmpty(orderData) || ObjectUtils.isEmpty(orderData.getInfo())) {
            log.error("Failed to add order data: {}", data);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = nhanhvnMapper.convertToOrderEntity(posId, orderData, PosName.WEBHOOK.getValue());
        if (ObjectUtils.isEmpty(orderEntity)) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderAdd] Failed to convert orderData={} to OrderEntity", orderData.getInfo().getId());
            return false;
        }

        // Save order
        orderRepository.save(orderEntity);
        log.info("[NhanhvnWebhookServiceImpl.handleOrderAdd] Successfully saved OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Convert OrderItemEntities
        List<OrderItemEntity> orderItemEntities = nhanhvnMapper.convertToOrderItemEntity(orderData, PosName.WEBHOOK.getValue());
        if (orderItemEntities.isEmpty()) {
            log.warn("[NhanhvnWebhookServiceImpl.handleOrderAdd] Order id={} has no products", orderEntity.getOrderId());
        } else {
            orderItemRepository.saveAll(orderItemEntities);
            log.info("[NhanhvnWebhookServiceImpl.handleOrderAdd] Successfully saved {} OrderItemEntities for orderId={}",
                    orderItemEntities.size(), orderEntity.getOrderId());
        }

        return true;
    }

    private boolean handleOrderUpdate(String posId, Object data) {
        NhanhvnOrderWebhookRequest orderData = objectMapper.convertValue(
                data, NhanhvnOrderWebhookRequest.class
        );
        log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Update OrderData: {}", orderData);

        if (ObjectUtils.isEmpty(orderData) || ObjectUtils.isEmpty(orderData.getInfo())) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Failed to update order data: {}", data);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = nhanhvnMapper.convertToOrderEntity(posId, orderData, PosName.WEBHOOK.getValue());
        if (ObjectUtils.isEmpty(orderEntity)) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Failed to convert orderData={} to OrderEntity", orderData.getInfo().getId());
            return false;
        }

        // Save (insert/update)
        orderRepository.save(orderEntity);
        log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Successfully updated OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Sync OrderItems
        List<OrderItemEntity> orderItemEntities = nhanhvnMapper.convertToOrderItemEntity(orderData, PosName.WEBHOOK.getValue());
        if (orderItemEntities.isEmpty()) {
            log.warn("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Order id={} has no products", orderEntity.getOrderId());
            return false;
        } else {
            // Xóa items cũ để tránh dữ liệu thừa
            orderItemRepository.deleteByOrderId(orderEntity.getOrderId());

            // Save lại items mới
            orderItemRepository.saveAll(orderItemEntities);
            log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Successfully synced {} OrderItemEntities for orderId={}",
                    orderItemEntities.size(), orderEntity.getOrderId());
        }

        return true;
    }

    private boolean handleOrderDelete(String posId, Object data) {
        List<String> ids = objectMapper.convertValue(data, new TypeReference<>() {
        });
        if (ids.isEmpty()) {
            log.warn("[NhanhvnWebhookServiceImpl.handleOrderDelete] No order ID provided for deletion");
            return false;
        }

        for (String orderId : ids) {
            Optional<OrderEntity> orderEntity = orderRepository.findById(orderId);
            if (orderEntity.isEmpty()) {
                log.warn("[NhanhvnWebhookServiceImpl.handleOrderDelete] Failed to find order with id={}", orderId);
                return false;
            }

            // Nếu xóa cả orderItem liên quan, làm trước khi xóa order
            List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
            if (!orderItems.isEmpty()) {
                orderItemRepository.deleteAll(orderItems);
                log.info("[NhanhvnWebhookServiceImpl.handleOrderDelete] Deleted {} order items for orderId={}", orderItems.size(), orderId);
            }

            orderRepository.deleteById(orderId);
            log.info("[NhanhvnWebhookServiceImpl.handleOrderDelete] Deleted order with id={} from posId={}", orderId, posId);
        }

        return true;
    }

    private boolean handleInventoryChange(String posId, Object data) {
        List<NhanhvnInventoryResponse> nhanhvnInventoryResponses = objectMapper.convertValue(
                data,
                new TypeReference<>() {
                }
        );

        for (NhanhvnInventoryResponse inventoryResponse : nhanhvnInventoryResponses) {
            VariantId variantId = new VariantId(String.valueOf(inventoryResponse.getId()), posId);
            Optional<ProductVariantEntity> variantEntity = variantRepository.findById(variantId);
            if (variantEntity.isEmpty()) {
                log.warn("[NhanhvnWebhookServiceImpl.handleInventoryChange] Failed to find variant with id={}", inventoryResponse.getId());
                return false;
            }

            ProductVariantEntity variant = variantEntity.get();
            variant.setInventoryQuantity(inventoryResponse.getRemain());
            variant.setFulfillableQuantity(inventoryResponse.getAvailable());
            variantRepository.save(variant);

            log.info("[NhanhvnWebhookServiceImpl.handleInventoryChange] Updated inventory for variantId={} posId={} inventoryQuantity={} fulfillableQuantity={}",
                    variant.getProductId(), posId, inventoryResponse.getRemain(), inventoryResponse.getAvailable());
        }
        return true;
    }

    private NhanhvnProductResponse.ProductData getProductById(String posId, String id) {
        try {
            // 1. Lấy posEntity từ DB
            PosEntity posEntity = posRepository.findById(posId)
                    .orElseThrow(() -> new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_NOTFOUND)));

            NhanhvnRequest request = NhanhvnRequest.buildRequest(
                    posEntity.getConfig(),
                    posEntity.getAccessToken()
            );
            request.setFilters(Map.of(NhanhvnConstants.IDS, id));
            Optional<NhanhvnProductResponse> responseOpt = nhanhvnClient.getProducts(request);

            if (responseOpt.isPresent() && ObjectUtils.isNotEmpty(responseOpt.get().getData())) {
                return responseOpt.get().getData().get(0);
            }

            // Không tìm thấy sản phẩm
            return null;
        } catch (Exception e) {
            log.error("[NhanhvnWebhookServiceImpl.getProductById] Failed for posId={}, productId={}, error={}", posId, id, e.getMessage(), e);
            return null;
        }
    }
}