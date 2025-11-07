package com.nemi.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.model.response.PageResponse;
import com.nemi.model.response.ProductBasicResponse;
import com.nemi.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client-api/v1")
@RequiredArgsConstructor
public class ProductClientApi {

    private final ProductService productService;

    @GetMapping("/search/product/ad-link")
    @RequirePermission("SETTINGS.AD_PRODUCT_LINK.VIEW")
    public ResponseEntity<PageResponse<ProductBasicResponse>> searchProductForAdLink(@RequestParam(value = "search", defaultValue = "") String search,
                                                                             @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                             @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ResponseEntity.ok(productService.clientSearchProduct(search, page, size));
    }

    @GetMapping("/search/product/ad-view")
    @RequirePermission("ADVERTISEMENT.AD_ACCOUNT_INFO.VIEW")
    public ResponseEntity<PageResponse<ProductBasicResponse>> searchProductForAdView(@RequestParam(value = "search", defaultValue = "") String search,
                                                                             @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                             @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ResponseEntity.ok(productService.clientSearchProduct(search, page, size));
    }
}
