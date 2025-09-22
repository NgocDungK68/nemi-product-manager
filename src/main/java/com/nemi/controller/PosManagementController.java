package com.nemi.controller;

import com.nemi.model.response.PosConnectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nemi-product-manager/public-api/v1")
public class PosManagementController {
    @GetMapping("/{posName}/auth")
    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName) {
        return null;
    }
}
