package com.ch.cloud.gateway.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 请求记录配置类
 * 
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.request-recorder")
@Data
public class RequestRecorderConfig {
    
    /**
     * 是否启用请求记录
     */
    private boolean enabled = true;
    
    /**
     * 跳过的URL路径
     */
    private List<String> skipUrls = Arrays.asList(
            "/upms/op/record/",
            "/upms/login/record/"
    );
    
    /**
     * 是否记录静态资源请求
     */
    private boolean recordStaticResources = false;
    
    /**
     * 是否记录WebSocket请求
     */
    private boolean recordWebSocket = false;
    
    /**
     * 是否记录GET请求
     */
    private boolean recordGetRequests = true;
    
    /**
     * 请求记录超时时间（毫秒）
     */
    private long timeout = 30000;
    
    /**
     * 是否启用请求体记录
     */
    private boolean recordRequestBody = true;
    
    /**
     * 是否启用响应体记录
     */
    private boolean recordResponseBody = true;
    
    /**
     * 请求体最大大小（字节）
     */
    private int maxRequestBodySize = 1024 * 1024; // 1MB
    
    /**
     * 响应体最大大小（字节）
     */
    private int maxResponseBodySize = 1024 * 1024; // 1MB
    
    /**
     * 是否启用敏感信息脱敏
     */
    private boolean enableSensitiveMasking = true;
    
    /**
     * 敏感字段列表
     */
    private List<String> sensitiveFields = Arrays.asList(
            "password", "token", "authorization", "secret"
    );
} 