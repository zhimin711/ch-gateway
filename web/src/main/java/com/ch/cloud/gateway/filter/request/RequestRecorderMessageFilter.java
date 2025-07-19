package com.ch.cloud.gateway.filter.request;

import com.ch.cloud.gateway.decorator.RecorderServerHttpRequestDecorator;
import com.ch.cloud.gateway.decorator.RecorderServerHttpResponseDecorator;
import com.ch.cloud.gateway.utils.GatewayLogUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

/**
 * 请求记录消息过滤器 将请求日志发送到RocketMQ
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Log4j2
public class RequestRecorderMessageFilter extends AbsRequestRecorderFilter {
    
    @Value("${rocketmq.enabled:false}")
    private Boolean mqOn;
    
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    public static final String MQ_TOPIC = "request-logs";
    
    @Override
    protected Mono<Void> filterLog(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查请求体大小
        if (isRequestBodyTooLarge(exchange.getRequest())) {
            log.warn("Request body too large, skipping recording: {}", exchange.getRequest().getURI());
            return chain.filter(exchange);
        }
        
        // 在 GatewayFilter 之前执行，此时的request是最初的request
        RecorderServerHttpRequestDecorator request = new RecorderServerHttpRequestDecorator(exchange.getRequest());
        
        // 此时的response是发送回客户端的response
        RecorderServerHttpResponseDecorator response = new RecorderServerHttpResponseDecorator(exchange.getResponse());
        
        ServerWebExchange ex = exchange.mutate().request(request).response(response).build();
        
        long startTimeMillis = System.currentTimeMillis();
        
        return GatewayLogUtil.recorderOriginalRequest(ex).then(Mono.defer(() -> chain.filter(ex)))
                .then(Mono.defer(() -> finishLog(ex, startTimeMillis))).timeout(Duration.ofMillis(config.getTimeout()))
                .onErrorResume(throwable -> {
                    log.error("Request recording failed: {}", exchange.getRequest().getURI(), throwable);
                    return chain.filter(exchange);
                });
    }
    
    /**
     * 完成日志记录
     */
    private Mono<Void> finishLog(ServerWebExchange ex, long startTimeMillis) {
        return GatewayLogUtil.recorderResponse(ex).doOnSuccess(x -> {
            try {
                long endTimeMillis = System.currentTimeMillis();
                String logStr = GatewayLogUtil.getLogData(ex, startTimeMillis, endTimeMillis);
                
                // 脱敏处理
                logStr = maskSensitiveInfo(logStr);
                
                if (mqOn && logStr != null) {
                    sendToMq(ex, logStr);
                } else {
                    log.info("request log:\n{}",logStr);
                }
            } catch (Exception e) {
                log.error("Failed to process request log: {}", ex.getRequest().getURI(), e);
            }
        }).onErrorResume(throwable -> {
            log.error("Failed to record response: {}", ex.getRequest().getURI(), throwable);
            return Mono.empty();
        });
    }
    
    /**
     * 发送日志到RocketMQ
     */
    private void sendToMq(ServerWebExchange ex, String logStr) {
        try {
            List<String> uList = ex.getRequest().getHeaders().getOrEmpty("X-Token-User");
            String topic = MQ_TOPIC;
            if (!uList.isEmpty()) {
                topic += ":" + uList.get(0);
            }
            
            // 使用异步发送，避免阻塞
            rocketMQTemplate.asyncSend(topic, logStr, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (log.isDebugEnabled()) {
                        log.debug("Request log sent successfully: {} => {}", ex.getRequest().getURI(),
                                sendResult.getMsgId());
                    }
                }
                
                @Override
                public void onException(Throwable e) {
                    log.error("Failed to send request log: {}", ex.getRequest().getURI(), e);
                }
            });
        } catch (Exception e) {
            log.error("Exception while sending to MQ: {}", ex.getRequest().getURI(), e);
        }
    }
    
    @Override
    public int getOrder() {
        // 在GatewayFilter之前执行
        return -1;
    }
}