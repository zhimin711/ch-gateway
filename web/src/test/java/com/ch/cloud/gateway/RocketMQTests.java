package com.ch.cloud.gateway;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class RocketMQTests {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Test
    public void send() {
        rocketMQTemplate.convertAndSend("demo","test1");
    }

    @Test
    public void receive(){
//        rocketMQTemplate.
    }
}
