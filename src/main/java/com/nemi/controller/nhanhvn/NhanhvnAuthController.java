package com.nemi.controller.nhanhvn;

import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.service.nhanhvn.NhanhvnAuthService;
import com.nemi.service_impl.nhanhvn.NhanhvnWebhookServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/nhanhvn/auth")
@RequiredArgsConstructor
@Slf4j
public class NhanhvnAuthController {
    private final NhanhvnAuthService nhanhvnAuthService;
    private final NhanhvnWebhookServiceImpl nhanhvnWebhookService;

    /**
     * Callback URL khi user login thành công bên Nhanh.vn
     * https://nhanh.vn/oauth?version=3.0&appId=76215&returnLink=https://nemi-dev-02.ecombase.net/redirect_url_1
     */
    @PostMapping("/access-token")
    public ResponseEntity<NhanhvnAccessTokenResponse> exchangeAccessToken(@RequestParam String accessCode) {
        return ResponseEntity.ok(
                nhanhvnAuthService.exchangeAccessToken(accessCode)
                        .orElseThrow(() -> new RuntimeException("Exchange access token failed!"))
        );
    }



}