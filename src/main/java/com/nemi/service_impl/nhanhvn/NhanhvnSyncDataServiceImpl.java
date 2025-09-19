package com.nemi.service_impl.nhanhvn;

import com.nemi.constant.enums.Platform;
import com.nemi.entity.ProductEntity;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.ProductRepository;
import com.nemi.service.nhanhvn.NhanhvnProductService;
import com.nemi.service.nhanhvn.NhanhvnSyncDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnSyncDataServiceImpl implements NhanhvnSyncDataService {
    private final NhanhvnProductService nhanhvnProductService; // gọi API Nhanh.vn
    private final ProductRepository productRepository;

    @Override
    public Mono<String> triggerSyncNhanhvnData() {
        log.info("Triggering sync Product for Nhanh.vn data");

        return Mono.fromCallable(() -> {
            try {
                List<ProductEntity> allProducts = new ArrayList<>();
                Map<String, Object> paginator = new HashMap<>();
                paginator.put("size", 50);

                boolean hasMorePages = true;

                while (hasMorePages) {
                    Optional<NhanhvnProductResponse> responseOpt =
                            nhanhvnProductService.getProducts(paginator);

                    if (responseOpt.isEmpty()) {
                        log.error("Failed to fetch products with paginator: {}", paginator);
                        break;
                    }

                    NhanhvnProductResponse response = responseOpt.get();

                    if (response.getData() == null || response.getData().isEmpty()) {
                        log.info("No products found with paginator: {}", paginator);
                        break;
                    }

                    List<ProductEntity> pageProducts = convertToProductEntities(response.getData());
                    allProducts.addAll(pageProducts);

                    log.info("Fetched {} products from Nhanh.vn, total so far: {}",
                            pageProducts.size(), allProducts.size());

                    // update paginator.next
                    if (response.getPaginator() != null && response.getPaginator().getNext() != null) {
                        paginator.put("next", response.getPaginator().getNext());
                        hasMorePages = true;
                    } else {
                        hasMorePages = false;
                    }
                }

                saveAllNhanhvnProductsSync(allProducts);

                String result = String.format(
                        "Successfully synced %d products from Nhanh.vn",
                        allProducts.size()
                );
                log.info(result);
                return result;

            } catch (Exception e) {
                String error = String.format("Failed to sync Nhanh.vn data - %s", e.getMessage());
                log.error(error, e);
                throw new RuntimeException(error, e);
            }
        }).doOnError(error -> log.error("Error in triggerSyncNhanhvnData: {}", error.getMessage(), error));
    }

    @Transactional
    public void saveAllNhanhvnProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Nhanh.vn products synchronously", products.size());

        if (products.isEmpty()) {
            return;
        }

        try {
            int batchSize = 100;
            for (int i = 0; i < products.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, products.size());
                List<ProductEntity> batch = products.subList(i, endIndex);

                productRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} products",
                        i + 1, endIndex, products.size());
            }

            log.info("Successfully saved all {} Nhanh.vn products", products.size());
        } catch (Exception e) {
            log.error("Failed to save Nhanh.vn products synchronously: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<ProductEntity> convertToProductEntities(List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(this::convertToProductEntity)
                .collect(Collectors.toList());
    }

    private ProductEntity convertToProductEntity(NhanhvnProductResponse.ProductData apiProducts) {
        ProductEntity product = new ProductEntity();

        product.setId(String.valueOf(apiProducts.getId()));
        product.setSku(apiProducts.getCode());
        product.setTitle(apiProducts.getName());
        product.setStatus(apiProducts.getStatus());
        product.setPlatform(Platform.NHANHVN);
        product.setCategoryId(
                apiProducts.getCategory() != null ? String.valueOf(apiProducts.getCategory().getId()) : null
        );

        if (apiProducts.getCreatedAt() != null) {
            product.setCreatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(apiProducts.getCreatedAt()), ZoneOffset.UTC));
        }
        if (apiProducts.getUpdatedAt() != null) {
            product.setUpdatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(apiProducts.getUpdatedAt()), ZoneOffset.UTC));
        }

        return product;
    }
}