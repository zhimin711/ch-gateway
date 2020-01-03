package com.ch.cloud.gateway.conf;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

/**
 * decs:
 *
 * @author 01370603
 * @date 2019/12/25
 */
@Configuration
public class RedisConfiguration {

    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        Config config = Config.fromYAML(new ClassPathResource("config/redisson-sentinel.yml").getInputStream());
        return Redisson.create(config);
    }

//    redis jar包整合有问题
    @Bean
    public RedisConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean("stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

}
