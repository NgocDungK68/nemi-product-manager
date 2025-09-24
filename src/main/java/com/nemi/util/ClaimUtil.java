package com.nemi.util;

import com.nemi.constant.ClaimName;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ClaimUtil {

    public String getUserId() {
        try {
            return "123";
//            return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        } catch (Exception e) {
            log.error("Exception occurred while getting username", e);
            return null;
        }
    }

    public String getUserName() {
        return getClaim(ClaimName.USER_NAME).orElseThrow(() -> new BadCredentialsException("UnAuthorized"));
    }

    public Integer getCompanyId() {
        return Integer.valueOf(getClaim(ClaimName.COMPANY).orElseThrow(() -> new BadCredentialsException("UnAuthorized")));
    }

    public Optional<String> getClaim(String claimName) {
        try {
            Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
            return claims.get(claimName, String.class).describeConstable();
        } catch (Exception e) {
            log.error("Exception occurred while getting {}", claimName, e);
            return Optional.empty();
        }
    }
}
