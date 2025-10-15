package com.nemi.controller;

import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.GeneralPosService;
import com.nemi.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final PosRepository posRepository;
    private final GeneralPosService generalPosService;
    private final EncryptionService encryptionService;

//    private final PosManagementService pancakeService;

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

    @PostMapping("/public-api/testReAuth/{posId}")
    public String testReAuth(@PathVariable String posId) {
        PosEntity posEntity = posRepository.findById(posId).orElse(null);
        return generalPosService.buildReAuthLink(posEntity);
    }

    @PostMapping("/public-api/pancake/{posId}")
    public ResponseEntity<String> connectPos(@PathVariable String posId) {

//        pancakeService.syncProduct(posId);
//        pancakeService.syncOrder(posId);
        return ResponseEntity.ok("xong");

    }

    @GetMapping("/public-api/test-decrypt-config/{posId}")
    public ResponseEntity<String> testDecryptConfig(@PathVariable String posId) {
                // Lấy POS entity từ database
            PosEntity posEntity = posRepository.findById(posId)
                    .orElseThrow(() -> new RuntimeException("POS not found with id: " + posId));
            
            // Decrypt config
            String decryptedConfig = encryptionService.decrypt(posEntity.getConfig());
            
            // Trả về kết quả
            return ResponseEntity.ok("Decrypted Config for POS " + posId + ":\n" + decryptedConfig);
            
    }
}
