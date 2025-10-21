package com.nemi.annotation;


import com.nemi.configuration.WebhookConfig;
import com.nemi.constant.PosConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CheckXRealIpAspect {

    private final WebhookConfig webhookConfig;

    @Around("@annotation(com.nemi.annotation.CheckXRealIp) || @within(com.nemi.annotation.CheckXRealIp)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // no request context -> allow (or you can block)
            return pjp.proceed();
        }

        HttpServletRequest request = attrs.getRequest();

        // find annotation (method first, then class)
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CheckXRealIp ann = method.getAnnotation(CheckXRealIp.class);
        if (ann == null) {
            ann = method.getDeclaringClass().getAnnotation(CheckXRealIp.class);
        }

        String configuredPartner = ann != null ? ann.partner() : "";

        // extract ip
        String ip = request.getHeader(PosConstants.X_REAL_IP);
        if (ip == null || ip.trim().isEmpty()) {
            String forwarded = request.getHeader(PosConstants.X_FORWARDED_FOR);
            if (forwarded != null) {
                ip = forwarded.split(",")[0].trim();
            }
        }
        if (ip == null || ip.trim().isEmpty()) {
            ip = request.getRemoteAddr();
        }

        // determine partner: use annotation value if provided, otherwise try to parse from path
        String partner = null;
        if (configuredPartner != null && !configuredPartner.trim().isEmpty()) {
            partner = configuredPartner.trim();
        } else {
            String path = request.getRequestURI();
            String[] segments = path.split("/");
            partner = segments.length > 3 ? segments[3] : null;
        }

        if (partner == null || partner.isEmpty()) {
            log.warn("Missing partner in request path while checking X-Real-IP");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing partner in path");
        }

        if (!webhookConfig.isAllowedIp(partner, ip)) {
            log.warn("Forbidden IP {} for partner {}", ip, partner);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IP not allowed");
        }

        return pjp.proceed();
    }
}