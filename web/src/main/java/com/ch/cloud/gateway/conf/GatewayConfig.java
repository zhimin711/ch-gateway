package com.ch.cloud.gateway.conf;


import com.ch.Constants;
import com.ch.utils.CommonUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Configuration
public class GatewayConfig {

    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    public KeyResolver clientKeyResolver() {
        return (exchange) -> {
            String clientKey = exchange.getRequest().getHeaders().getFirst(Constants.X_TOKEN);
            if (CommonUtils.isEmpty(clientKey)) {
                clientKey = getIP(exchange.getRequest());
            }
            return Mono.just(clientKey);
        };
    }


    private static String getIP(ServerHttpRequest request) {
        if (request == null) return "N/A";
        String ip = request.getHeaders().getFirst("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("x-Original-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if ((ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) && request.getRemoteAddress() != null) {
            ip = request.getRemoteAddress().getHostName();
        }
        return ip;
    }
}
