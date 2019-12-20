package com.ch.cloud.gateway.conf;


import com.ch.Constants;
import com.ch.utils.CommonUtils;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver clientKeyResolver() {
        return (exchange) -> {
            String clientKey = exchange.getRequest().getHeaders().getFirst(Constants.TOKEN_HEADER2);
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
