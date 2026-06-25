package com.lyy.gateway.filter;

import com.lyy.common.properties.JwtProperties;
import com.lyy.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局 JWT 认证过滤器
 * 职责：校验 Token → 提取 userId → 写入请求头 X-User-Id 转发给下游微服务
 * 注意：Gateway 基于 WebFlux，不能使用传统的 HttpServletRequest/Response
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * 白名单路径：不需要校验 Token（这些是前端请求路径）
     */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/video/page",
            "/api/video/search",
            "/api/video/partitions"
    );

    /**
     * 文件流 GET 请求白名单（上传 POST 仍需鉴权）
     */
    private static final String FILE_STREAM_PATH = "/api/video/file/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        boolean isWhitelisted = WHITE_LIST.stream().anyMatch(path::contains);
        // 文件流 GET 请求放行，上传 POST 仍需鉴权
        boolean isFileStream = path.contains(FILE_STREAM_PATH)
                && request.getMethod().name().equals("GET");

        if (isWhitelisted || isFileStream) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 从请求头获取 Token
        String token = request.getHeaders().getFirst(jwtProperties.getUserTokenName());
        log.info("请求头名称: {}, Token 值: {}", jwtProperties.getUserTokenName(), 
                token != null ? token.substring(0, 20) + "..." : "null");

        if (token == null || token.trim().isEmpty()) {
            log.warn("请求未携带 Token，路径: {}", path);
            return writeUnauthorized(exchange, "未登录，请先登录");
        }

        // 解析 Token
        log.info("Gateway 使用的密钥: {}", jwtProperties.getUserSecretKey());
        Claims claims = jwtUtil.parseUserToken(token);
        if (claims == null) {
            log.warn("Token 校验失败（过期/非法/签名错误），路径: {}", path);
            return writeUnauthorized(exchange, "登录已过期，请重新登录");
        }

        // 提取 userId 并写入转发请求头
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            log.warn("Token 中未包含 userId 字段，路径: {}", path);
            return writeUnauthorized(exchange, "Token 数据异常");
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId.toString())
                .build();

        log.debug("Token 校验通过，userId: {}, 路径: {}", userId, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 返回 401 未授权 JSON 响应
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
