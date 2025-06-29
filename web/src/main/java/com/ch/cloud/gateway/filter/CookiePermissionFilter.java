package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.cloud.gateway.conf.CookieConfig;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.service.CookieRefreshService;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Cookie权限过滤器
 * 处理支持Cookie token的路径，并支持Cookie自动刷新
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class CookiePermissionFilter extends AbstractPermissionFilter {

    @Autowired
    private CookieRefreshService cookieRefreshService;
    
    @Autowired
    private CookieConfig cookieConfig;

    @Override
    protected int getFilterOrder() {
        return -180; // 在登录权限之前执行
    }

    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("Cookie权限检查: {}", path);
        
        // 从Cookie中获取token
        String cookieToken = getCookieToken(exchange.getRequest());
        if (CommonUtils.isNotEmpty(cookieToken)) {
            // 检查并刷新Cookie
            if (cookieRefreshService.needRefreshCookie(cookieToken)) {
                log.debug("Cookie即将过期，开始刷新，路径: {}", path);
                cookieRefreshService.refreshCookie(exchange.getResponse(), cookieToken);
            }
            
            // 将Cookie token添加到请求头中，供后续过滤器使用
            ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                    .header(Constants.X_TOKEN, cookieToken)
                    .build();
            ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
            log.debug("从Cookie中获取到token，路径: {}", path);
            return chain.filter(mutableExchange);
        }
        
        // 没有Cookie token，继续下一个过滤器
        return chain.filter(exchange);
    }

    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 如果已经被白名单处理，则跳过
        return false;
    }

    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Collection<PermissionDto> cookiePermissions = getPermissions(CacheType.PERMISSIONS_COOKIE_LIST, null);
        
        if (!cookiePermissions.isEmpty()) {
            boolean isCookieSupported = checkPermissions(cookiePermissions, path, exchange.getRequest().getMethod());
            if (isCookieSupported) {
                log.debug("路径 {} 支持Cookie token", path);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 从Cookie中获取token
     */
    private String getCookieToken(ServerHttpRequest request) {
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        HttpCookie cookie = cookies.getFirst(cookieConfig.getTokenName());
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }
} 