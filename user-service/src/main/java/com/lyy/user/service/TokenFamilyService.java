package com.lyy.user.service;

import com.lyy.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface TokenFamilyService {

    String issueAccessToken(Long userId);

    String issueRefreshToken(Long userId);

    Result<Map<String, String>> refresh(String rawRefreshToken,
                                         HttpServletRequest request,
                                         HttpServletResponse response);

    void revokeAllFamily(Long userId);

    void setRefreshTokenCookie(HttpServletResponse response, String refreshToken);

    void clearRefreshTokenCookie(HttpServletResponse response);
}
