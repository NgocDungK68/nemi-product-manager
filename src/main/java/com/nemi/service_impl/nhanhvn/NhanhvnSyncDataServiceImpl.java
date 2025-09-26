package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.constant.enums.PosName;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.service.nhanhvn.NhanhvnProductService;
import com.nemi.service.sync_data.PosSyncDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NhanhvnSyncDataServiceImpl implements PosSyncDataService {
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final NhanhvnProductService nhanhvnProductService;
    private final ObjectMapper objectMapper;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public boolean trigger(String posId) {
        try {
            // B1: lấy PosEntity và validate posName
            PosEntity posEntity = getPos(posId);

            // B2: parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {}
            );

            String appId = configMap.get("appId");
            String businessId = configMap.get("businessId");
            String accessToken = posEntity.getAccessToken();

            if (appId == null || businessId == null || accessToken == null) {
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();

            // chỉ set size cho lần đầu
            Map<String, Object> paginator = new HashMap<>();
            paginator.put("size", 50);

            NhanhvnRequest request = NhanhvnRequest.builder()
                    .appId(appId)
                    .businessId(businessId)
                    .accessToken(accessToken)
                    .paginator(paginator)
                    .build();

            while (true) {
                Optional<NhanhvnProductResponse> responseOpt = nhanhvnProductService.getProducts(request);

                if (responseOpt.isEmpty()) {
                    log.error("Failed to fetch products with paginator: {}", paginator);
                    return false;
                }

                NhanhvnProductResponse response = responseOpt.get();

                if (response.getData() == null || response.getData().isEmpty()) {
                    log.info("No products found with paginator: {}", paginator);
                    break;
                }

                List<ProductEntity> pageProducts = convertToProductEntities(response.getData());
                allProducts.addAll(pageProducts);

                log.info("Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

                // xử lý next
                if (response.getPaginator() != null && response.getPaginator().getNext() != null) {
                    paginator.clear(); // reset để chỉ giữ next
                    paginator.put("next", response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }

            saveAllProductsSync(allProducts);

            log.info("Successfully synced {} products from Nhanh.vn", allProducts.size());
            return true;

        } catch (Exception e) {
            log.error("Failed to sync Nhanh.vn data - {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void saveAllProductsSync(List<ProductEntity> products) {
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

    @Override
    public PosEntity getPos(String posId) {
        log.debug("[NhanhvnSyncDataImpl.getPos] posId: {}", posId);

        // Lấy PosEntity từ DB
        return posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.error("Error [NhanhvnSyncDataImpl.getPos] not found posId: {}", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
                });
    }

    private List<ProductEntity> convertToProductEntities(List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(this::convertToProductEntity)
                .collect(Collectors.toList());
    }

    private ProductEntity convertToProductEntity(NhanhvnProductResponse.ProductData apiProducts) {
        ProductEntity product = new ProductEntity();

        product.setProductId(String.valueOf(apiProducts.getId()));
        product.setCode(apiProducts.getCode());
        product.setName(apiProducts.getName());
        product.setStatus(apiProducts.getStatus().toString());

        return product;
    }
}
