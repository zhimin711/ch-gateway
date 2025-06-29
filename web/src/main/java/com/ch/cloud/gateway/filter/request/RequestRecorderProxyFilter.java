package com.ch.cloud.gateway.filter.request;

import com.ch.cloud.gateway.decorator.RecorderServerHttpRequestDecorator;
import com.ch.cloud.gateway.utils.GatewayLogUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 请求记录代理过滤器
 * 记录路由请求信息
 * 
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Log4j2
public class RequestRecorderProxyFilter extends AbsRequestRecorderFilter {

    @Override
    protected Mono<Void> filterLog(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 在 NettyRoutingFilter 之前执行，基本上属于倒数第二个过滤器了
        // 此时的request是经过各种转换、转发之后的request
        // 对应日志中的代理请求部分
        
        try {
            RecorderServerHttpRequestDecorator request = new RecorderServerHttpRequestDecorator(exchange.getRequest());
            
            ServerWebExchange ex = exchange.mutate()
                    .request(request)
                    .build();

            return GatewayLogUtil.recorderRouteRequest(ex)
                    .then(Mono.defer(() -> chain.filter(ex)))
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .onErrorResume(throwable -> {
                        log.error("Proxy request recording failed: {}", exchange.getRequest().getURI(), throwable);
                        return chain.filter(exchange);
                    });
        } catch (Exception e) {
            log.error("Exception in proxy request recording: {}", exchange.getRequest().getURI(), e);
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        // 在向业务服务转发前执行 NettyRoutingFilter 或 WebClientHttpRoutingFilter
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
