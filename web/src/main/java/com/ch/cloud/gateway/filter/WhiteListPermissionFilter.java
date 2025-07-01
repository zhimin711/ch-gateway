package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.utils.GatewayConstants;
import com.ch.cloud.upms.dto.PermissionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 白名单权限过滤器 处理不需要认证的路径
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class WhiteListPermissionFilter extends AbstractPermissionFilter {
    
    @Override
    protected int getFilterOrder() {
        return -200; // 最高优先级，最先执行
    }
    
    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        // 如果路径在白名单中，则跳过后续过滤器
        String path = exchange.getRequest().getURI().getPath();
        log.debug("白名单权限检查: {}", path);
        Collection<PermissionDto> whiteList = getPermissions(CacheType.PERMISSIONS_WHITE_LIST, null);
        boolean skipAfter = false;
        if (!whiteList.isEmpty()) {
            boolean isWhiteList = checkPermissions(whiteList, path, exchange.getRequest().getMethod());
            if (isWhiteList) {
                log.debug("路径 {} 在白名单中，可跳过后面的过滤器", path);
                skipAfter = true;
            }
        }
        // 添加跳过后续过滤器
        ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                .header(GatewayConstants.FILTER_HEADER_SKIP_AFTER, skipAfter ? "true" : "false").build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
        return chain.filter(mutableExchange);
    }
    
    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 不跳过任何请求，让当前过滤器处理校验是否在白名单中
        return false;
    }
    
    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        // 白名单处理已在shouldSkip中完成，这里不需要再处理
        return true;
    }
} 