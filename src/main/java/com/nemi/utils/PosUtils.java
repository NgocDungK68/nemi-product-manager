package com.nemi.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.constant.PancakeConstatns;
import com.nemi.constant.PosConstants;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.service.EncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j

public class PosUtils {
    PosUtils() {
    }

    private static final ObjectMapper mapper = new ObjectMapper();


    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            headers.put(key, request.getHeader(key));
        }
        return headers;
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (ObjectUtils.isEmpty(dateTimeString) || ObjectUtils.isEmpty(dateTimeString.trim())) {
            return null;
        }

        try {
            // 1. Dùng Instant để xử lý chuỗi ISO 8601 có 'Z' (Zulu/UTC)
            // Instant.parse() xử lý định dạng "yyyy-MM-ddTHH:mm:ssZ" hoặc có mili giây.
            Instant instant = Instant.parse(dateTimeString.trim());

            // 2. Chuyển Instant (UTC time) sang LocalDateTime (bỏ thông tin múi giờ)
            // Sử dụng ZoneOffset.UTC để đảm bảo chuyển đổi chính xác từ UTC.
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

        } catch (Exception e) {
            log.warn("Failed to parse date time '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        }
    }

    public static Map<String, String> convertToConfigMap(String config) {
        try {
            return mapper.readValue(
                    config,
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            log.error("Failed to convert to Config Map: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }

    // LocalDate to Long
    public static long toEpochSecond(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        try {
            return dateTime.toEpochSecond(ZoneOffset.of(PosConstants.VIETNAM_ZONE));
        } catch (Exception e) {
            log.error("Failed to convert LocalDateTime to epoch seconds: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATE_CONVERSION_ERROR));
        }
    }

    public static LocalDateTime pancakeParseTime(String time){

        if (ObjectUtils.isEmpty(time) || ObjectUtils.isEmpty(time.trim())) {
            return null;
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(PancakeConstatns.PANCAKE_TIME_FORMAT);


        LocalDateTime insertedAt = LocalDateTime.parse(
                time, formatter);


        ZonedDateTime vietnamTime = insertedAt
                .atZone(ZoneId.of(PosConstants.UTC))
                .withZoneSameInstant(ZoneId.of(PosConstants.VIETNAM_TIMEZONE));

        return  vietnamTime.toLocalDateTime();

    }



    // Long to LocalDate
    public static LocalDateTime convertEpochSecondsToVNTime(Long epochSeconds) {
        if (epochSeconds == null) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSeconds),
                ZoneId.of(PosConstants.VIETNAM_TIMEZONE)
        );
    }
}
