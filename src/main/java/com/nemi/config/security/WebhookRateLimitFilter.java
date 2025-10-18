package com.nemi.config.security;


import com.nemi.config.WebhookWhitelistConfig;
import com.nemi.config.SlidingWindowCounterRateLimiter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebhookRateLimitFilter implements Filter {

    private final WebhookWhitelistConfig whitelistConfig;
    private final Map<String, SlidingWindowCounterRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public WebhookRateLimitFilter(WebhookWhitelistConfig whitelistConfig) {
        this.whitelistConfig = whitelistConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String ip = extractClientIp(httpReq,httpRes);
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
    }


    private String extractClientIp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ip = request.getHeader("x-real-ip");

        if (ip == null || ip.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required header");
            return null;
        }

        return ip.trim();
    }
}
