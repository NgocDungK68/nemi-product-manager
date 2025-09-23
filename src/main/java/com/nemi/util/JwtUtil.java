// khi nao lam viec voi jwttoken thi xu li sau



//package com.nemi.util;
//
//import com.nemi.config.SecretConfig;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.Jws;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.MalformedJwtException;
//import io.jsonwebtoken.UnsupportedJwtException;
//import io.jsonwebtoken.io.Decoders;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.security.KeyFactory;
//import java.security.NoSuchAlgorithmException;
//import java.security.PublicKey;
//import java.security.spec.InvalidKeySpecException;
//import java.security.spec.X509EncodedKeySpec;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class JwtUtil {
//
//    private final SecretConfig secretConfig;
//
//    PublicKey getSigningKey() {
//        try {
//            byte[] keyBytes = Decoders.BASE64.decode(secretConfig.getJwtPublicKey());
//            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
//            return keyFactory.generatePublic(keySpec);
//        } catch (NoSuchAlgorithmException e) {
//            log.error("RSA algorithm not available: {}", e.getMessage());
//            throw new RuntimeException("Failed to create public key", e);
//        } catch (InvalidKeySpecException e) {
//            log.error("Invalid key specification: {}", e.getMessage());
//            throw new RuntimeException("Failed to create public key", e);
//        }
//    }
//
//
//    public Jws<Claims> extractToken(String token) {
//        try {
//            return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
//        } catch (MalformedJwtException e) {
//            log.error("Invalid JWT token: {}", e.getMessage());
//        } catch (ExpiredJwtException e) {
//            log.error("JWT token is expired: {}", e.getMessage());
//        } catch (UnsupportedJwtException e) {
//            log.error("JWT token is unsupported: {}", e.getMessage());
//        } catch (IllegalArgumentException e) {
//            log.error("JWT claims string is empty: {}", e.getMessage());
//        }
//
//        return null;
//    }
//}
