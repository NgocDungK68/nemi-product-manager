package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties("crypto")
public class CryptoConfig {
    private String password;
    private String mode; //GCM or CTR
    private int strength;    // 128, 192, 256

}


