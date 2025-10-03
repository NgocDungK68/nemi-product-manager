package com.nemi.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
    private final NhanhvnConfig nhanhvnConfig;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("nhanhvnRestTemplate")
    public RestTemplate nhanhvnRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
                nhanhvnConfig.getBaseUrl() + "/" + nhanhvnConfig.getApiVersion() + "/")
        );

        return restTemplate;
    }
}
