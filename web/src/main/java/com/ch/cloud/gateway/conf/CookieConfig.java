package com.ch.cloud.gateway.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cookie配置类
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.cookie")
@Data
public class CookieConfig {

    /**
     * Cookie名称
     */
    private String tokenName = "TOKEN";



    /**
     * Cookie最大存活时间（秒）
     */
    private int maxAge = 1800; // 30分钟

    /**
     * Cookie刷新阈值（秒）- 在过期前多少秒开始刷新
     */
    private int refreshThreshold = 300; // 5分钟

    /**
     * Cookie路径
     */
    private String path = "/";

    /**
     * 是否仅HTTP访问
     */
    private boolean httpOnly = true;

    /**
     * 是否仅HTTPS访问
     */
    private boolean secure = false;

    /**
     * 是否启用Cookie自动刷新
     */
    private boolean autoRefresh = true;

    /**
     * 是否启用Cookie刷新日志
     */
    private boolean enableLog = true;
}
