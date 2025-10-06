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
    private final PancakeConfig pancakeConfig;

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
    @Bean("pancakeRestTemplate")
    public RestTemplate pancakeRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
                pancakeConfig.getBaseUrl() + "/shops/")
        );

        return restTemplate;
    }

    // Note: Sapo doesn't need a dedicated RestTemplate bean because each merchant
    // has a different storeName (e.g., store1.mysapo.net, store2.mysapo.net)
    // So we build the full URL dynamically in SapoClient instead
}
