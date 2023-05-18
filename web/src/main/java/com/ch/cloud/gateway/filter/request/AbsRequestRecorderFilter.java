package com.ch.cloud.gateway.filter.request;

import com.ch.cloud.gateway.decorator.RecorderServerHttpRequestDecorator;
import com.ch.cloud.gateway.utils.GatewayLogUtil;
import com.ch.cloud.gateway.utils.PathConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Log4j2
public abstract class AbsRequestRecorderFilter implements GlobalFilter, Ordered {

    String[] skipUrls = {"/upms/op/record/","/upms/login/record/"};
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();

        URI originalRequestUri = originalRequest.getURI();
        
        //只记录http的请求
        String scheme = originalRequestUri.getScheme();
        if ((!"http".equals(scheme) && !"https".equals(scheme))) {
            return chain.filter(exchange);
        }

        String upgrade = originalRequest.getHeaders().getUpgrade();
        if ("websocket".equalsIgnoreCase(upgrade)) {
            return chain.filter(exchange);
        }
        RequestPath path = originalRequest.getPath();
        for (String skip : skipUrls) {
            if (path.value().startsWith(skip)) {
                return chain.filter(exchange);
            }
        }

        if (originalRequest.getMethod() == HttpMethod.GET && isStaticResource(originalRequestUri)) {
            return chain.filter(exchange);
        }

        return filterLog(exchange, chain);
    }

    protected boolean isStaticResource(URI originalRequestUri) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        boolean isDownload = pathMatcher.match(PathConstants.DOWNLOAD_PATTERN, originalRequestUri.getPath());
        boolean isImages = pathMatcher.match(PathConstants.IMAGES_PATTERN, originalRequestUri.getPath());
        return isDownload || isImages;
    }

    protected abstract Mono<Void> filterLog(ServerWebExchange exchange, GatewayFilterChain chain);
}
