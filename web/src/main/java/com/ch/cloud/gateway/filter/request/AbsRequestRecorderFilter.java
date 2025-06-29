package com.ch.cloud.gateway.filter.request;

import com.ch.cloud.gateway.conf.RequestRecorderConfig;
import com.ch.cloud.gateway.utils.PathConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 请求记录过滤器抽象基类
 * 
 * @author zhimi
 * @since 2024-1-1
 */
@Log4j2
public abstract class AbsRequestRecorderFilter implements GlobalFilter, Ordered {

    @Autowired
    protected RequestRecorderConfig config;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查是否启用请求记录
        if (!config.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest originalRequest = exchange.getRequest();
        URI originalRequestUri = originalRequest.getURI();
        
        // 只记录http/https的请求
        String scheme = originalRequestUri.getScheme();
        if ((!"http".equals(scheme) && !"https".equals(scheme))) {
            return chain.filter(exchange);
        }

        // 检查WebSocket请求
        String upgrade = originalRequest.getHeaders().getUpgrade();
        if ("websocket".equalsIgnoreCase(upgrade) && !config.isRecordWebSocket()) {
            return chain.filter(exchange);
        }

        // 检查跳过的URL
        RequestPath path = originalRequest.getPath();
        if (shouldSkipUrl(path.value())) {
            return chain.filter(exchange);
        }

        // 检查静态资源
        if (originalRequest.getMethod() == HttpMethod.GET && 
            isStaticResource(originalRequestUri) && 
            !config.isRecordStaticResources()) {
            return chain.filter(exchange);
        }

        // 检查GET请求
        if (originalRequest.getMethod() == HttpMethod.GET && !config.isRecordGetRequests()) {
            return chain.filter(exchange);
        }

        return filterLog(exchange, chain);
    }

    /**
     * 检查是否应该跳过该URL
     */
    protected boolean shouldSkipUrl(String path) {
        for (String skip : config.getSkipUrls()) {
            if (path.startsWith(skip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为静态资源
     */
    protected boolean isStaticResource(URI originalRequestUri) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        boolean isDownload = pathMatcher.match(PathConstants.DOWNLOAD_PATTERN, originalRequestUri.getPath());
        boolean isImages = pathMatcher.match(PathConstants.IMAGES_PATTERN, originalRequestUri.getPath());
        return isDownload || isImages;
    }

    /**
     * 获取请求大小（字节）
     */
    protected long getRequestSize(ServerHttpRequest request) {
        String contentLength = request.getHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                log.warn("Invalid Content-Length header: {}", contentLength);
            }
        }
        return 0;
    }

    /**
     * 检查请求体是否过大
     */
    protected boolean isRequestBodyTooLarge(ServerHttpRequest request) {
        return getRequestSize(request) > config.getMaxRequestBodySize();
    }

    /**
     * 脱敏处理
     */
    protected String maskSensitiveInfo(String content) {
        if (!config.isEnableSensitiveMasking() || content == null) {
            return content;
        }
        
        String masked = content;
        for (String field : config.getSensitiveFields()) {
            // 简单的脱敏处理，实际项目中可以使用更复杂的正则表达式
            masked = masked.replaceAll("(?i)" + field + "\\s*[:=]\\s*[^\\s,}]+", 
                    field + "=***");
        }
        return masked;
    }

    /**
     * 具体的日志记录逻辑，由子类实现
     */
    protected abstract Mono<Void> filterLog(ServerWebExchange exchange, GatewayFilterChain chain);
}
