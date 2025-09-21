//package com.nemi.service_impl.pancake;
//
//import com.nemi.constant.enums.Platform;
//import com.nemi.entity.ProductEntity;
//import com.nemi.entity.ProductImageEntity;
//import com.nemi.model.response.pancake.PancakeProductsResponse;
//import com.nemi.repository.ProductRepository;
//import com.nemi.service.pancake.PancakeService;
//import com.nemi.service.pancake.PancakeSyncDataService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import reactor.core.publisher.Mono;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class PancakeSyncDataImpl implements PancakeSyncDataService {
//
//    @Autowired
//    private PancakeService pancakeService;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//
//    @Override
//    public Mono<String> triggerSyncPancakeData(String shopId) {
//        log.info("Triggering sync Product for Pancake data for shopId: {}", shopId);
//
//        return Mono.fromCallable(() -> {
//            try {
//                List<ProductEntity> allProducts = new ArrayList<>();
//                int currentPage = 1;
//                int pageSize = 30; // Default page size from API
//                boolean hasMorePages = true;
//
//                // Fetch all pages
//                while (hasMorePages) {
//                    Map<String, String> queryParams = new HashMap<>();
//                    queryParams.put("page", String.valueOf(currentPage));
//                    queryParams.put("per_page", String.valueOf(pageSize));
//
//                    Optional<PancakeProductsResponse> responseOpt = pancakeService.getProducts(shopId, queryParams);
//
//                    if (responseOpt.isEmpty()) {
//                        log.error("Failed to fetch products for page: {}", currentPage);
//                        break;
//                    }
//
//                    PancakeProductsResponse response = responseOpt.get();
//
//                    if (response.getData() == null || response.getData().isEmpty()) {
//                        log.info("No more products found on page: {}", currentPage);
//                        break;
//                    }
//
//                    // Convert API response to entities
//                    List<ProductEntity> pageProducts = convertToProductEntities(response.getData());
//                    allProducts.addAll(pageProducts);
//
//                    log.info("Fetched {} products from page {}/{}",
//                            pageProducts.size(), currentPage, response.getTotalPages());
//
//                    // Check if there are more pages
//                    hasMorePages = currentPage < response.getTotalPages();
//                    currentPage++;
//                }
//
//                // Save all products to database
//                saveAllPancakeProductsSync(allProducts);
//
//                String result = String.format("Successfully synced %d products from Pancake for shopId: %s",
//                        allProducts.size(), shopId);
//                log.info(result);
//                return result;
//
//            } catch (Exception e) {
//                String error = String.format("Failed to sync Pancake data for shopId: %s - %s", shopId, e.getMessage());
//                log.error(error, e);
//                throw new RuntimeException(error, e);
//            }
//        }).doOnError(error -> log.error("Error in triggerSyncPancakeData: {}", error.getMessage(), error));
//    }
//
//    @Override
//    @Transactional
//    public void saveAllPancakeProducts(List<ProductEntity> products) {
//        log.info("Saving {} products asynchronously", products.size());
//
//        if (products.isEmpty()) {
//            return;
//        }
//
//        try {
//            // Batch save products
//            List<ProductEntity> savedProducts = productRepository.saveAll(products);
//            log.info("Successfully saved {} products to database", savedProducts.size());
//        } catch (Exception e) {
//            log.error("Failed to save products: {}", e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    @Override
//    @Transactional
//    public void saveAllPancakeProductsSync(List<ProductEntity> products) {
//        log.info("Saving {} products synchronously", products.size());
//
//        if (products.isEmpty()) {
//            return;
//        }
//
//        try {
//            // Optional: Clear existing products for full sync
//            // productRepository.deleteAll();
//            // log.info("Cleared existing products for full sync");
//
//            // Batch save products
//            int batchSize = 100;
//            for (int i = 0; i < products.size(); i += batchSize) {
//                int endIndex = Math.min(i + batchSize, products.size());
//                List<ProductEntity> batch = products.subList(i, endIndex);
//
//                productRepository.saveAll(batch);
//                log.info("Saved batch {}-{} of {} products", i + 1, endIndex, products.size());
//            }
//
//            log.info("Successfully saved all {} products to database", products.size());
//        } catch (Exception e) {
//            log.error("Failed to save products synchronously: {}", e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    /**
//     * Convert Pancake API products to ProductEntity objects
//     */
//    private List<ProductEntity> convertToProductEntities(List<PancakeProductsResponse.Product> apiProducts) {
//        return apiProducts.stream()
//                .map(this::convertToProductEntity)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Convert single Pancake API product to ProductEntity
//     */
//    private ProductEntity convertToProductEntity(PancakeProductsResponse.Product apiProduct) {
//        ProductEntity product = new ProductEntity();
//
//        // Basic product info
//        product.setId(apiProduct.getId());
//        product.setSku(apiProduct.getCustomId());
//        product.setTitle(apiProduct.getName());
//        product.setDescription(apiProduct.getDescription());
//
//        // Category - use first category if available
//        if (apiProduct.getCategoryIds() != null && !apiProduct.getCategoryIds().isEmpty()) {
//            product.setCategoryId(apiProduct.getCategoryIds().get(0).toString());
//        }
//
//        // Status mapping: published = 1, not published = 0
//        product.setStatus(apiProduct.getIsPublished() != null && apiProduct.getIsPublished() ? 1 : 0);
//
//        // Brand name from keyword field
//        product.setBrandName(extractBrandFromKeyword(apiProduct.getKeyword()));
//
//        // Timestamps
//        product.setCreatedAt(apiProduct.getInsertedAt());
//        product.setUpdatedAt(apiProduct.getUpdatedAt());
//
//        // Process images
//        List<ProductImageEntity> images = extractProductImages(apiProduct, product);
//        product.setImages(images);
//        product.setPlatform(Platform.PANCAKE);
//
//        return product;
//    }
//
//    /**
//     * Extract brand name from keyword field
//     */
//    private String extractBrandFromKeyword(String keyword) {
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return null;
//        }
//
//        // Keyword format seems to be "vai|cao cấp" or similar
//        // You can customize this logic based on your brand extraction needs
//        String[] parts = keyword.split("\\|");
//        if (parts.length > 1) {
//            return parts[1].trim(); // Return second part as brand
//        }
//
//        return keyword.trim(); // Return whole keyword as brand
//    }
//
//    /**
//     * Extract and create ProductImageEntity objects from API product
//     */
//    private List<ProductImageEntity> extractProductImages(PancakeProductsResponse.Product apiProduct, ProductEntity product) {
//        List<ProductImageEntity> images = new ArrayList<>();
//
//        // Main product image
//        if (apiProduct.getImage() != null && !apiProduct.getImage().trim().isEmpty()) {
//            ProductImageEntity mainImage = new ProductImageEntity();
//            mainImage.setProduct(product);
//            mainImage.setUrl(apiProduct.getImage());
//            mainImage.setSortOrder(0);
//            images.add(mainImage);
//        }
//
//        // Variation images
//        if (apiProduct.getVariations() != null) {
//            int sortOrder = 1;
//            for (PancakeProductsResponse.Variation variation : apiProduct.getVariations()) {
//                if (variation.getImages() != null) {
//                    for (String imageUrl : variation.getImages()) {
//                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
//                            ProductImageEntity varImage = new ProductImageEntity();
//                            varImage.setProduct(product);
//                            varImage.setUrl(imageUrl);
//                            varImage.setSortOrder(sortOrder++);
//                            images.add(varImage);
//                        }
//                    }
//                }
//            }
//        }
//
//        return images;
//    }
//}