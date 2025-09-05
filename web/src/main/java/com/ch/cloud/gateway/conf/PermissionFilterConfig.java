package com.ch.cloud.gateway.conf;

import com.ch.cloud.gateway.filter.*;
import com.ch.cloud.gateway.service.FeignClientHolder;
import com.ch.cloud.gateway.utils.UserAuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 权限过滤器配置类 统一管理所有权限过滤器
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class PermissionFilterConfig {

    @Autowired
    private FeignClientHolder feignClientHolder;

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        // 初始化工具类
        UserAuthUtils.setFeignClientHolder(feignClientHolder);
        UserAuthUtils.setRedissonClient(redissonClient);
        log.info("权限过滤器配置初始化完成");
    }

    /**
     * 白名单权限过滤器
     */
    @Bean
    public WhiteListPermissionFilter whiteListPermissionFilter() {
        return new WhiteListPermissionFilter();
    }

    /**
     * 授权码权限过滤器
     */
    @Bean
    public AuthCodePermissionFilter authCodePermissionFilter() {
        return new AuthCodePermissionFilter();
    }

    /**
     * Cookie权限过滤器
     */
    @Bean
    public CookiePermissionFilter cookiePermissionFilter() {
        return new CookiePermissionFilter();
    }

    /**
     * 登录权限过滤器
     */
    @Bean
    public LoginPermissionFilter loginPermissionFilter() {
        return new LoginPermissionFilter();
    }

    /**
     * 角色权限过滤器
     */
    @Bean
    public RolePermissionFilter rolePermissionFilter() {
        return new RolePermissionFilter();
    }
}
