package com.nemi.model.response.nhanhvn.managershop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopResponse {
    private List<Shop> shops;
    private boolean success;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shop {
        @JsonProperty("avatar_url")
        private String avatarUrl;
        private String currency;
        private Long id;
        private List<LinkPostMaker> linkPostMakers;
        private String name;
        private List<Page> pages;
    }

    public static class LinkPostMaker {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        private String id;
        @JsonProperty("is_onboard_xendit") //trường này để kiểm tra shop đã onboard xendit chưa
        private boolean isOnboardXendit;
        private String platform;
        @JsonProperty("progressive_catalog_error")
        private String progressiveCatalogError;
        private Settings settings;
        private long shop_id;
        private List<Tag> tags;
        private String username;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Settings {
        private Boolean auto_create_order;
        @JsonProperty("current_settings_key")
        private String currentSettingsKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private String color;
        private int id;
        @JsonProperty("lighten_color")
        private String lightenColor;
        private String text;
    }
}

