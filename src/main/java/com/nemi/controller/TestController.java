package com.nemi.controller;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.service.PosManagementService;
import com.nemi.service_impl.pancake.PancakeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final PancakeServiceImpl pancakeService;


    @GetMapping("/test")
    public String test() {
        return "Test successful";
    }

    @GetMapping("/public-api/test")
    public String test2() {
        return "Test successful";
    }

    @GetMapping("/service-api/test")
    public String testService() {
        return "Test service api successful";
    }

    @PostMapping("/public-api/pancake/{posId}")
    public ResponseEntity<String> connectPos(@PathVariable String posId) {

        pancakeService.syncProduct(posId);
        pancakeService.syncOrder(posId);
        return ResponseEntity.ok("xong");

    }
}
