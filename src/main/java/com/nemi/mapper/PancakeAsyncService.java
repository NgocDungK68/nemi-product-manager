package com.nemi.mapper;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.enums.Status;
import com.nemi.enums.WeightUnit;
import com.nemi.model.response.pancake.PancakeProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PancakeAsyncService {
    @Async("syncExecutor")
    protected CompletableFuture<List<ProductEntity>> convertToProductEntities(
            String posId,
            List<PancakeProductResponse.ProductData> apiProducts
    ) {
        List<ProductEntity> result = apiProducts.stream()  // hoặc parallelStream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(result);
    }

    protected ProductEntity convertToProductEntity(String posId, PancakeProductResponse.ProductData apiProducts) {

        String images = Optional.ofNullable(apiProducts.getImages())
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(",")))
                .orElse(null);

        String categories = Optional.ofNullable(apiProducts.getProduct())
                .map(PancakeProductResponse.Product::getCategories)
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .map(PancakeProductResponse.Category::getName)
                        .collect(Collectors.joining(",")))
                .orElse(null);



        ProductEntity product = ProductEntity.builder()
                .posId(posId)
                .productId(String.valueOf(apiProducts.getId()))
                .code(apiProducts.getProduct().getDisplayId())
                .name(apiProducts.getProduct().getName())
                .productId(apiProducts.getProductId())
                .description(apiProducts.getProduct().getNoteProduct())
                .images(images)
                .category(categories)
                .build();
        if (apiProducts.getIsLocked()) {
            product.setStatus(Status.INACTIVE.getValue());
        } else {
            product.setStatus(Status.ACTIVE.getValue());
        }
        return product;
    }

    @Async("syncExecutor")
    protected CompletableFuture<List<ProductVariantEntity>> convertToVariantEntities(
            String posId,
            List<PancakeProductResponse.ProductData> apiProducts
    ) {
        List<ProductVariantEntity> result = apiProducts.stream()  // can nhac paralle stream
                .map(apiProduct -> convertToVariantEntity(posId, apiProduct))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(result);
    }



    public ProductVariantEntity convertToVariantEntity(String posId, PancakeProductResponse.ProductData apiProduct) {

        Integer remainQuantity = Optional.ofNullable(apiProduct.getVariationsWarehouses())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(PancakeProductResponse.VariationWarehouse::getRemainQuantity)
                .orElse(null);


        return ProductVariantEntity.builder()
                .variantId(apiProduct.getId())
                .posId(posId)
                .productId(String.valueOf(apiProduct.getProductId()))
                .sku(apiProduct.getDisplayId())
                .barcode(apiProduct.getBarcode())
                .price(BigDecimal.valueOf(apiProduct.getRetailPrice()))
                .inventoryQuantity(apiProduct.getRemainQuantity())
                .fulfillableQuantity(remainQuantity)
                .weight(apiProduct.getWeight())
                .weightUnit(WeightUnit.GAM.getValue())
                .build();
    }
}
