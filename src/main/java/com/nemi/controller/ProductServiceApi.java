package com.nemi.controller;

import com.nemi.model.response.ProductBasicResponse;
import com.nemi.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/service-api/v1")
@RequiredArgsConstructor
public class ProductServiceApi {

    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<List<ProductBasicResponse>> searchProducts(@RequestParam("companyId") Integer companyId,
                                                                    @RequestParam(value = "productIds", required = false) List<String> productIds,
                                                                    @RequestParam(value = "skus", required = false) List<String> skus) {
        return ResponseEntity.ok(productService.searchProducts(companyId, productIds, skus));
    }
}
