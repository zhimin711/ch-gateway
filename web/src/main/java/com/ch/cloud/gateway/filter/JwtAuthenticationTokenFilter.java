package com.ch.cloud.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 权限过滤器 - 已废弃
 * 
 * 该类已被重构为多个独立的权限过滤器：
 * - WhiteListPermissionFilter: 白名单权限过滤器
 * - CookiePermissionFilter: Cookie权限过滤器  
 * - LoginPermissionFilter: 登录权限过滤器
 * - RolePermissionFilter: 角色权限过滤器
 * 
 * 请使用新的权限过滤器架构
 *
 * @author zhimi
 * @since 2020-1-1
 * @deprecated 已废弃，请使用新的权限过滤器架构
 */
@Configuration
@Slf4j
@Deprecated
public class JwtAuthenticationTokenFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 已废弃的过滤器，直接放行，由新的权限过滤器处理
        log.warn("使用已废弃的JwtAuthenticationTokenFilter，建议迁移到新的权限过滤器架构");
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -50; // 降低优先级，让新的过滤器先执行
    }
}
