package com.nemi.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

@Slf4j
public class JsonUtils {

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final DecimalFormat df = new DecimalFormat("#.##");
    public static final String AD_ACCOUNT_PREFIX = "act_";

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T map(Object object, Class<T> tClass) {
        try {
            return mapper.convertValue(object, tClass);
        } catch (Exception e) {
            log.error("map error");
            return null;
        }
    }
    
    public static <T> T fromJson(String json, Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (Exception e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("JSON serialization error: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String formatDecimal(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }
        decimal = decimal.setScale(2, RoundingMode.HALF_UP);
        return df.format(decimal);
    }

    public static String formatDecimal(Double decimal) {
        BigDecimal bd = new BigDecimal(decimal);
        return formatDecimal(bd);
    }

    public static String removeAdAccountPrefix(String adAccountId) {
        if (adAccountId == null || !adAccountId.startsWith(AD_ACCOUNT_PREFIX)) {
            return adAccountId;
        }
        return adAccountId.replaceFirst(AD_ACCOUNT_PREFIX, "");
    }

    public static String addAdAccountPrefix(String adAccountId) {
        if (adAccountId == null || adAccountId.startsWith(AD_ACCOUNT_PREFIX)) {
            return adAccountId;
        }
        return AD_ACCOUNT_PREFIX + adAccountId;
    }
}
