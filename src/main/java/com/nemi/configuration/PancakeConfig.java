package com.nemi.configuration;

import com.nemi.enums.Status;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

@Data
@Configuration
@ConfigurationProperties("pancake")
public class PancakeConfig {
    private String baseUrl;
    private String apiKey;
    private String shopId;
    private int timeout;
    private Sync sync;
    private String xApiKey;

    @Data
    public static class Retry {
        private int maxAttempts;
        private int backoffDelay;
    }


    //---status-----
    private Order order;

    @Data
    public static class Order {
        private Status status;

        @Data
        public static class Status {
            private Map<Integer, String> mapping;
        }
    }

    @Data
    public static class Sync {
        private int order;
        private int orderItem;
        private int product;
        private int pageStart;
    }

    public String getStatusMapping(Integer key,String statusName) {

        return Optional.ofNullable(key)
                .map(code -> this.getOrder().getStatus().getMapping()
                        .getOrDefault(code,statusName))
                .orElse(Status.UNKNOWN.getValue());
    }

}
