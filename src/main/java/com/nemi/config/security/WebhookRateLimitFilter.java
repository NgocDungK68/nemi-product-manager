package com.nemi.config.security;


import com.nemi.config.SlidingWindowCounterRateLimiter;
import com.nemi.config.WebhookWhitelistConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebhookRateLimitFilter extends OncePerRequestFilter {

    private final WebhookWhitelistConfig whitelistConfig;
    private final Map<String, SlidingWindowCounterRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public WebhookRateLimitFilter(WebhookWhitelistConfig whitelistConfig) {
        this.whitelistConfig = whitelistConfig;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        log.info("WebhookRateLimitFilter processing request: {}", request.getRequestURI());
        if (request.getRequestURI().startsWith("/public-api/")
                || request.getRequestURI().startsWith("/service-api/")
                || request.getRequestURI().startsWith("/client-api/")
                || request.getRequestURI().startsWith("/actuator/")) {
            log.trace("Bypass public path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }


        try {

            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpRes = (HttpServletResponse) response;

            String ip = extractClientIp(httpReq);
            String path = httpReq.getRequestURI();

            // cho URL dạng: /webhook/v1/{partner}/...
            String[] segments = path.split("/");
            String partner = segments.length > 3 ? segments[3] : null;

            if (partner == null) {
                log.warn("Missing partner in webhook path: {}", path);
                httpRes.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing partner in path");
                return;
            }


            if (!whitelistConfig.isAllowedIp(partner, ip)) {
                log.warn(" Forbidden IP {} for partner {}", ip, partner);
                httpRes.sendError(HttpServletResponse.SC_FORBIDDEN, "IP not allowed");
                return;
            }
// rate limite torng milisec
            SlidingWindowCounterRateLimiter limiter = rateLimiters.computeIfAbsent(
                    partner,
                    p -> new SlidingWindowCounterRateLimiter(60, 60_000, 1_000)
                    // 60 req / 1m, 1 segmend = 1s = 1*10^3 ms
            );

            if (!limiter.allowRequest()) {
                log.warn(" Rate limit exceeded for partner {} (IP: {})", partner, ip);
                httpRes.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded");
                return;
            }
            log.info("✅ WebhookRateLimitFilter passed for partner={} from IP={}", partner, ip);

            //  3. Cho phép request đi tiếp
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Exception occurred while processing authentication token");
        }
    }

    private String extractClientIp(HttpServletRequest request) throws IOException {
        String ip = request.getHeader("x-real-ip");

        if (StringUtils.isEmpty(ip)) {
            ip = request.getHeader("x-forwarded-for");
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }


        if (StringUtils.isEmpty(ip)) {
            ip = request.getRemoteAddr(); // fallback cuối cùng
        }

        return ip;
    }


}
