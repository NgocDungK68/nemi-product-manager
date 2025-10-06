package com.nemi.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class PosUtils {
    public static String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while (!ObjectUtils.isEmpty((line = reader.readLine()))) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading request body", e);
        }
        return sb.toString();
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
}
