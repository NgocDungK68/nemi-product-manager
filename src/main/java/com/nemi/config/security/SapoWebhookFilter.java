package com.nemi.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter để cache raw request body cho Sapo webhook requests.
 * 
 * Vấn đề cần giải quyết:
 * - Sapo gửi webhook với HMAC signature tính từ raw JSON gốc
 * - Spring parse @RequestBody thành Object rồi serialize lại → JSON khác
 * - JSON serialize lại có thể khác format (0 vs 0.0, null vs "", spacing...)
 * - HMAC verification fail vì hash(JSON_gốc) ≠ hash(JSON_serialize_lại)
 * 
 * Giải pháp:
 * 1. Filter này chạy TRƯỚC khi Spring parse @RequestBody
 * 2. Cache raw JSON bytes từ HTTP request stream
 * 3. Lưu vào request attribute để SapoAuthChecker dùng
 * 4. Wrap request để cho phép đọc body nhiều lần (filter + controller)
 * 
 * Flow:
 * Request → SapoWebhookFilter (cache raw JSON) 
 *        → Spring MVC (parse @RequestBody) 
 *        → @PreAuthorize (verify HMAC từ raw JSON đã cache)
 *        → Controller method
 * 
 * Chỉ áp dụng cho: /webhook/v1/sapo/**
 */
@Slf4j
@Component
public class SapoWebhookFilter extends OncePerRequestFilter {
    
    /**
     * Key để lưu raw body vào request attribute.
     * SapoAuthChecker sẽ đọc từ attribute này để verify HMAC.
     */
    public static final String CACHED_RAW_BODY_ATTRIBUTE = "CACHED_RAW_BODY";

    /**
     * Filter logic: cache raw body nếu là Sapo webhook request.
     * 
     * @param request HTTP request
     * @param response HTTP response  
     * @param filterChain filter chain để pass request tiếp
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Chỉ cache body cho Sapo webhook requests
        if (request.getRequestURI().contains("/webhook/v1/sapo/")) {
            log.debug("Caching raw body for Sapo webhook request: {}", request.getRequestURI());
            
            // 1. Wrap request với CachedBodyHttpServletRequest để có thể đọc body nhiều lần
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            
            // 2. Đọc raw bytes và convert sang String UTF-8
            String rawBody = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);
            
            // 3. Lưu raw body vào request attribute để SapoAuthChecker dùng sau
            cachedRequest.setAttribute(CACHED_RAW_BODY_ATTRIBUTE, rawBody);
            
            // 4. Pass cachedRequest (không phải request gốc) đi tiếp
            filterChain.doFilter(cachedRequest, response);
        } else {
            // Không phải Sapo webhook → pass request gốc
            filterChain.doFilter(request, response);
        }
    }
}
