package com.ch.cloud.gateway.filter.request;


import com.ch.Constants;
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
import java.util.List;

@Configuration
@Log4j2
public class RequestRecorderMessageFilter extends AbsRequestRecorderFilter {

    @Value("${rocketmq.enabled:false}")
    private Boolean mqOn;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    protected Mono<Void> filterLog(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 在 GatewayFilter 之前执行， 此时的request时最初的request
        RecorderServerHttpRequestDecorator request = new RecorderServerHttpRequestDecorator(exchange.getRequest());

        // 此时的response时 发送回客户端的 response
        RecorderServerHttpResponseDecorator response = new RecorderServerHttpResponseDecorator(exchange.getResponse());

        ServerWebExchange ex = exchange.mutate()
                .request(request)
                .response(response)
                .build();
        long startTimeMillis = System.currentTimeMillis();
        return GatewayLogUtil.recorderOriginalRequest(ex)
                .then(Mono.defer(() -> chain.filter(ex)))
                .then(Mono.defer(() -> finishLog(ex, startTimeMillis)));
    }

    public static final String MQ_TOPIC = "request-logs";

    private Mono<Void> finishLog(ServerWebExchange ex, long startTimeMillis) {
        return GatewayLogUtil.recorderResponse(ex)
                .doOnSuccess(x -> {
                    long endTimeMillis = System.currentTimeMillis();
                    String logStr = GatewayLogUtil.getLogData(ex, startTimeMillis, endTimeMillis);
                    List<String> uList = ex.getRequest().getHeaders().getOrEmpty(Constants.X_TOKEN_USER);
                    String topic = MQ_TOPIC;
                    if (!uList.isEmpty()) {
                        topic += ":" + uList.get(0);
                    }
//                    rocketMQTemplate.convertAndSend("request-logs", logStr);
                    if (mqOn)

                        rocketMQTemplate.asyncSend(topic, logStr, new SendCallback() {
                            @Override
                            public void onSuccess(SendResult sendResult) {
                                log.info("{} => {}", ex.getRequest().getURI(), sendResult.getMsgId());
                            }

                            @Override
                            public void onException(Throwable e) {
                                log.error("send error: " + ex.getRequest().getURI(), e);
                            }
                        });
                });
    }

    @Override
    public int getOrder() {
        //在GatewayFilter之前执行
        return -1;
    }
}