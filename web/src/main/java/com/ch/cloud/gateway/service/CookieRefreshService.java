package com.ch.cloud.gateway.service;

import com.ch.cloud.gateway.conf.CookieConfig;
import com.ch.cloud.gateway.pojo.CacheType;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Cookie刷新服务
 * 
 * @author zhimi
 * @since 2024-1-1
 */
@Service
@Slf4j
public class CookieRefreshService {
    
    @Autowired
    private CookieConfig cookieConfig;
    
    @Resource
    private org.redisson.api.RedissonClient redissonClient;
    
    /**
     * 检查Cookie是否需要刷新
     * 
     * @param token 用户token
     * @return 是否需要刷新
     */
    public boolean needRefreshCookie(String token) {
        if (!cookieConfig.isAutoRefresh()) {
            return false;
        }
        
        try {
            // 从Redis中获取用户信息
            String md5 = com.ch.utils.EncryptUtils.md5(token);
            RBucket<Object> userBucket = redissonClient.getBucket(
                    CacheType.GATEWAY_TOKEN.getKey(md5), JsonJacksonCodec.INSTANCE);
            
            if (!userBucket.isExists()) {
                return false;
            }
            
            Object user = userBucket.get();
            if (user == null) {
                return false;
            }
            
            // 通过反射获取过期时间
            try {
                java.lang.reflect.Method getExpireAtMethod = user.getClass().getMethod("getExpireAt");
                Object expireAt = getExpireAtMethod.invoke(user);
                
                if (expireAt == null) {
                    return false;
                }
                
                // 计算剩余时间
                long currentTime = System.currentTimeMillis();
                long expireTime = (Long) expireAt;
                long timeToExpire = expireTime - currentTime;
                
                // 如果token在阈值时间内过期，则需要刷新
                boolean needRefresh = timeToExpire <= cookieConfig.getRefreshThreshold() * 1000;
                
                if (needRefresh && cookieConfig.isEnableLog()) {
                    log.debug("Cookie即将过期，剩余时间: {}秒", timeToExpire / 1000);
                }
                
                return needRefresh;
                
            } catch (Exception e) {
                log.error("获取用户过期时间失败", e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("检查Cookie刷新状态时发生错误", e);
            return false;
        }
    }
    
    /**
     * 刷新Cookie
     * 
     * @param response HTTP响应对象
     * @param token 用户token
     */
    public void refreshCookie(ServerHttpResponse response, String token) {
        try {
            // 创建新的Cookie
            HttpCookie newCookie = new HttpCookie(cookieConfig.getTokenName(), token);
            
            // 设置Cookie属性
            newCookie.setMaxAge(cookieConfig.getMaxAge());
            newCookie.setPath(cookieConfig.getPath());
            newCookie.setHttpOnly(cookieConfig.isHttpOnly());
            newCookie.setSecure(cookieConfig.isSecure());
            
            // 添加Cookie到响应头
            response.addCookie(newCookie);
            
            if (cookieConfig.isEnableLog()) {
                log.debug("Cookie已刷新，名称: {}, 过期时间: {}秒", 
                        cookieConfig.getTokenName(), cookieConfig.getMaxAge());
            }
            
        } catch (Exception e) {
            log.error("刷新Cookie时发生错误", e);
        }
    }
    
    /**
     * 清除Cookie
     * 
     * @param response HTTP响应对象
     */
    public void clearCookie(ServerHttpResponse response) {
        try {
            HttpCookie cookie = new HttpCookie(cookieConfig.getTokenName(), "");
            cookie.setMaxAge(0); // 立即过期
            cookie.setPath(cookieConfig.getPath());
            cookie.setHttpOnly(cookieConfig.isHttpOnly());
            cookie.setSecure(cookieConfig.isSecure());
            
            response.addCookie(cookie);
            
            if (cookieConfig.isEnableLog()) {
                log.debug("Cookie已清除: {}", cookieConfig.getTokenName());
            }
            
        } catch (Exception e) {
            log.error("清除Cookie时发生错误", e);
        }
    }
    
    /**
     * 获取Cookie配置信息
     * 
     * @return Cookie配置
     */
    public CookieConfig getCookieConfig() {
        return cookieConfig;
    }
} 