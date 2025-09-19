package com.nemi.controller.pancake;

import com.nemi.service.pancake.PancakeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/pancake")
public class PancakeController {
    private final PancakeService pancakeService;

    public PancakeController(PancakeService pancakeService) {
        this.pancakeService = pancakeService;
    }

    @GetMapping("/{shopId}/orders/{orderId}")
    Optional<?> getOrderDetail(@PathVariable String shopId, @PathVariable String orderId) {
        return pancakeService.getOrderDetail(shopId, orderId, new java.util.HashMap<>());
    }

    @GetMapping("/shop")
    Optional<?> getShopInfo() {
        return pancakeService.getShopInfo(new java.util.HashMap<>());
    }

    @GetMapping("/sales-analytics")
    Optional<?> getSaleAnalytics() {
        return pancakeService.getSaleAnalytics(new java.util.HashMap<>());
    }
}
