package com.nemi.controller.sapo;

import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.service.sapo.SapoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sapo/auth")
@RequiredArgsConstructor
public class SapoAuthController {
    private final SapoAuthService sapoAuthService;

    @PostMapping("/access-token")
    public ResponseEntity<SapoAccessTokenResponse> exchangeAccessToken(@RequestParam String code) {
        return ResponseEntity.ok(
                sapoAuthService.getAccessToken(code)
                        .orElseThrow(() -> new RuntimeException("Exchange access token failed!"))
        );
    }
}