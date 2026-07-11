package com.lyy.user.service.impl;

import com.alibaba.fastjson2.JSON;
import com.lyy.common.constant.JwtClaimsConstant;
import com.lyy.common.constant.RedisKey;
import com.lyy.common.result.Result;
import com.lyy.common.utils.JwtUtil;
import com.lyy.user.entity.dto.TokenFamily;
import com.lyy.user.service.TokenFamilyService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenFamilyServiceImpl implements TokenFamilyService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String issueAccessToken(Long userId) {
        return jwtUtil.generateAccessToken(userId);
    }

    @Override
    public String issueRefreshToken(Long userId) {
        String familyId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(userId, familyId);

        TokenFamily family = TokenFamily.builder()
                .userId(userId)
                .currentTokenHash(sha256(refreshToken))
                .createdAt(System.currentTimeMillis() / 1000)
                .status(TokenFamily.ACTIVE)
                .build();

        String key = RedisKey.REFRESH_TOKEN_FAMILY_PREFIX + familyId;
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(family), Duration.ofDays(7));

        log.info("创建 Token Family: familyId={}, userId={}", familyId, userId);
        return refreshToken;
    }

    @Override
    public Result<Map<String, String>> refresh(String rawRefreshToken,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        Claims claims = jwtUtil.parseRefreshToken(rawRefreshToken);
        if (claims == null) {
            return Result.tokenExpired("Refresh Token 无效或已过期，请重新登录");
        }

        String tokenType = claims.get(JwtClaimsConstant.TOKEN_TYPE, String.class);
        if (!"REFRESH".equals(tokenType)) {
            log.warn("非 Refresh Token 传入刷新接口，type={}", tokenType);
            return Result.tokenExpired("令牌类型错误，请重新登录");
        }

        Long userId = claims.get(JwtClaimsConstant.USER_ID, Long.class);
        String familyId = claims.get(JwtClaimsConstant.FAMILY_ID, String.class);

        String key = RedisKey.REFRESH_TOKEN_FAMILY_PREFIX + familyId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            log.warn("Token Family 不存在或已过期: familyId={}", familyId);
            return Result.tokenExpired("登录会话已过期，请重新登录");
        }

        TokenFamily family = JSON.parseObject(json, TokenFamily.class);

        if (TokenFamily.REVOKED.equals(family.getStatus())) {
            log.warn("Token Family 已被撤销: familyId={}, userId={}", familyId, userId);
            return Result.tokenExpired("该账号已在其他地方登录或令牌已失效，请重新登录");
        }

        String tokenHash = sha256(rawRefreshToken);
        if (family.getUsedTokens().contains(tokenHash)) {
            log.error("检测到 Refresh Token 复用攻击！familyId={}, userId={}", familyId, userId);
            family.setStatus(TokenFamily.REVOKED);
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(family), Duration.ofDays(7));
            return Result.tokenExpired("检测到异常登录活动，请重新登录");
        }

        if (!tokenHash.equals(family.getCurrentTokenHash())) {
            log.warn("Refresh Token 不匹配当前有效 RT: familyId={}", familyId);
            return Result.tokenExpired("令牌已失效，请重新登录");
        }

        family.getUsedTokens().add(family.getCurrentTokenHash());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, familyId);
        family.setCurrentTokenHash(sha256(newRefreshToken));
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(family), Duration.ofDays(7));

        log.info("Refresh Token 轮换成功: familyId={}, userId={}", familyId, userId);

        String newAccessToken = jwtUtil.generateAccessToken(userId);
        setRefreshTokenCookie(response, newRefreshToken);

        Map<String, String> data = Map.of("accessToken", newAccessToken);
        return Result.success(data);
    }

    @Override
    public void revokeAllFamily(Long userId) {
        var keys = stringRedisTemplate.keys(RedisKey.REFRESH_TOKEN_FAMILY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                String json = stringRedisTemplate.opsForValue().get(key);
                if (json != null) {
                    TokenFamily family = JSON.parseObject(json, TokenFamily.class);
                    if (userId.equals(family.getUserId()) && TokenFamily.ACTIVE.equals(family.getStatus())) {
                        family.setStatus(TokenFamily.REVOKED);
                        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(family), Duration.ofDays(7));
                        log.info("撤销 Token Family: key={}, userId={}", key, userId);
                    }
                }
            }
        }
    }

    @Override
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/user/refresh")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/user/refresh")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
