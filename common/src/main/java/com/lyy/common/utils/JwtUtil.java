package com.lyy.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import com.lyy.common.properties.JwtProperties;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * 最新稳定版 JWT 工具类
 * 支持双密钥（管理端/用户端）生成与解析
 * 基于 JJWT 0.12.6，线程安全
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 生成管理端令牌
     * @param claims 自定义载荷（建议包含 userId、角色等）
     * @return JWT 字符串
     */
    public String generateAdminToken(Map<String, Object> claims) {
        return generateToken(claims, jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl());
    }

    /**
     * 生成用户端令牌
     * @param claims 自定义载荷
     * @return JWT 字符串
     */
    public String generateUserToken(Map<String, Object> claims) {
        return generateToken(claims, jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl());
    }

    /**
     * 解析管理端令牌
     * @param token JWT 字符串
     * @return 载荷 Claims，若解析失败（过期、非法等）返回 null
     */
    public Claims parseAdminToken(String token) {
        return parseToken(token, jwtProperties.getAdminSecretKey());
    }

    /**
     * 解析用户端令牌
     * @param token JWT 字符串
     * @return 载荷 Claims，若解析失败返回 null
     */
    public Claims parseUserToken(String token) {
        return parseToken(token, jwtProperties.getUserSecretKey());
    }

    /**
     * 通用令牌生成方法
     * @param claims    载荷
     * @param secretKey 密钥（字符串形式）
     * @param ttlSeconds 有效期（秒）
     * @return JWT
     */
    private String generateToken(Map<String, Object> claims, String secretKey, long ttlSeconds) {
        SecretKey key = getSecretKey(secretKey);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlSeconds * 1000);

        log.info("生成 Token 使用的密钥: {}", secretKey);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 通用令牌解析方法
     * @param token     JWT 字符串
     * @param secretKey 密钥
     * @return Claims，失败返回 null 并记录日志
     */
    private Claims parseToken(String token, String secretKey) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("尝试解析空的 JWT");
            return null;
        }
        try {
            SecretKey key = getSecretKey(secretKey);
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 参数非法: {}", e.getMessage());
        } catch (Exception e) {
            log.error("解析 JWT 发生未知异常", e);
        }
        return null;
    }

    /**
     * 将字符串密钥转换为 SecretKey 对象（HS256）
     * 注意：密钥长度需 ≥ 32 字符，否则抛出异常
     */
    private SecretKey getSecretKey(String secretKey) {
        if (secretKey == null || secretKey.length() < 32) {
            throw new IllegalArgumentException("JWT 密钥长度必须至少为 32 个字符（当前长度：" +
                    (secretKey == null ? 0 : secretKey.length()) + "）");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}