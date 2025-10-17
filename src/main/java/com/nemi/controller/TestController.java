package com.nemi.controller;

import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.GeneralPosService;
import com.nemi.service.EncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
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

    @PostMapping("/pancake/test")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        // 🧾 Log headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        log.info("🟣 [TestWebhook] Headers: {}", headers);

        // 🔍 Log query parameters
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (!parameterMap.isEmpty()) {
            Map<String, Object> queryParams = new HashMap<>();
            parameterMap.forEach((k, v) -> queryParams.put(k, String.join(",", v)));
            log.info("🟢 [TestWebhook] Query Params: {}", queryParams);
        } else {
            log.info("🟢 [TestWebhook] No query parameters");
        }

        // 📦 Log raw body
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        log.info("🔵 [TestWebhook] Body: {}", body);

        // 🌐 Log server info (domain, scheme, port, URI)
        String scheme = request.getScheme(); // http hoặc https
        String serverName = request.getServerName(); // domain hoặc hostname
        int serverPort = request.getServerPort(); // cổng
        String requestURI = request.getRequestURI(); // path
        String requestURL = request.getRequestURL().toString(); // full URL

        log.info("🌍 [TestWebhook] Server Info:");
        log.info("   Scheme: {}", scheme);
        log.info("   Server Name: {}", serverName);
        log.info("   Server Port: {}", serverPort);
        log.info("   Request URI: {}", requestURI);
        log.info("   Request URL: {}", requestURL);

        return ResponseEntity.ok("Webhook received successfully");
    }

}
