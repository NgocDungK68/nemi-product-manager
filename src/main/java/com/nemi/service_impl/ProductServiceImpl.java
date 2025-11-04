package com.nemi.service_impl;

import com.nemi.entity.ProductEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.response.PageResponse;
import com.nemi.model.response.ProductBasicResponse;
import com.nemi.repository.ProductRepository;
import com.nemi.service.ProductService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ClaimUtil claimUtil;

    @Override
    public List<ProductBasicResponse> searchProducts(Integer companyId, List<String> productIds, List<String> skus) {
        try {
            // Use custom query with join to efficiently filter by productIds or SKUs
            List<ProductEntity> products = productRepository.searchByProductIdsOrSkus(companyId, productIds, skus);

            // Map to response
            return products.stream()
                    .map(this::mapToProductBasicResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Unexpected error while searching products", e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public PageResponse<ProductBasicResponse> clientSearchProduct(String search, Integer page, Integer pageSize) {
        try {
            String departmentId = claimUtil.getDepartmentId();

            PageRequest pageRequest = PageRequest.of(page, pageSize);

            Page<ProductEntity> pageProduct = productRepository.clientSearch(search, departmentId, pageRequest);

            List<ProductBasicResponse> products = pageProduct.getContent().stream()
                    .map(this::mapToProductBasicResponse)
                    .collect(Collectors.toList());

            PageResponse<ProductBasicResponse> rs = new PageResponse<>();
            rs.setTotalElements(pageProduct.getTotalElements());
            rs.setTotalPages(pageProduct.getTotalPages());
            rs.setNumberOfElements(pageProduct.getNumberOfElements());
            rs.setData(products);

            return rs;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while searching products", e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private ProductBasicResponse mapToProductBasicResponse(ProductEntity product) {
        return ProductBasicResponse.builder()
                .id(product.getProductId())
                .name(product.getName())
                .image(product.getImages())
                .build();
    }
}

