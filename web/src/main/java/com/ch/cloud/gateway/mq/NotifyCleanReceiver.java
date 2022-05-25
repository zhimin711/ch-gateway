package com.ch.cloud.gateway.mq;

import com.ch.pojo.KeyValue;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 描述：
 *
 * @author Zhimin.Ma
 * @since 2022/5/25
 */
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@Component
@RocketMQMessageListener(consumerGroup = "${spring.application.name}-${spring.profiles.active}", topic = "gateway-clean")
public class NotifyCleanReceiver implements RocketMQListener<KeyValue> {


    @Override
    public void onMessage(KeyValue keyValue) {

    }
}
