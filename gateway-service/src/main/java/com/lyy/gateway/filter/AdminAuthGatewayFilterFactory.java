package com.lyy.gateway.filter;

import com.lyy.common.constant.JwtClaimsConstant;
import com.lyy.common.properties.JwtProperties;
import com.lyy.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端鉴权过滤器（路由级）
 * 只挂在两条 admin 路由上，校验 Admin-Token 后注入 X-Admin-Id / X-Admin-Name。
 * 在路由 YAML 中以 `- AdminAuth` 引用。
 */
@Slf4j
@Component
public class AdminAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AdminAuthGatewayFilterFactory.Config> {

    /** 登录接口免校验（此时还没有 Token） */
    private static final String LOGIN_PATH = "/api/admin/login";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public AdminAuthGatewayFilterFactory(JwtUtil jwtUtil, JwtProperties jwtProperties) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (path.startsWith(LOGIN_PATH)) {
                return chain.filter(exchange);
            }

            String token = request.getHeaders().getFirst(jwtProperties.getAdminTokenName());
            if (token == null || token.trim().isEmpty()) {
                log.warn("管理端请求未携带 Admin-Token，路径: {}", path);
                return writeUnauthorized(exchange, "未登录，请先登录");
            }

            Claims claims = jwtUtil.parseAdminToken(token);
            if (claims == null) {
                log.warn("Admin-Token 校验失败（过期/非法/签名错误），路径: {}", path);
                return writeUnauthorized(exchange, "管理员登录已过期");
            }

            Long adminId = claims.get(JwtClaimsConstant.EMP_ID, Long.class);
            if (adminId == null) {
                log.warn("Admin-Token 中未包含 empId，路径: {}", path);
                return writeUnauthorized(exchange, "Token 数据异常");
            }

            ServerHttpRequest.Builder builder = request.mutate()
                    .header("X-Admin-Id", adminId.toString());

            String adminName = claims.get(JwtClaimsConstant.NAME, String.class);
            if (adminName != null && !adminName.isEmpty()) {
                // HTTP 头只能安全承载 ISO-8859-1，中文姓名必须编码，
                // 由 AdminContextInterceptor 负责解码还原
                builder.header("X-Admin-Name",
                        URLEncoder.encode(adminName, StandardCharsets.UTF_8));
            }

            log.debug("管理端鉴权通过，adminId: {}, 路径: {}", adminId, path);
            return chain.filter(exchange.mutate().request(builder.build()).build());
        };
    }

    /**
     * 返回 HTTP 200 + code=401。
     * 沿用现有 JwtAuthGlobalFilter 的风格（不用 401 状态码），
     * 便于前端统一按 body 的 code 判断，也与管理端 401001 区分开。
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"msg\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** 无参数配置，占位以符合 AbstractGatewayFilterFactory 的泛型要求 */
    public static class Config {
    }
}
