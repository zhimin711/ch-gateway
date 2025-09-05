package com.ch.cloud.gateway.service;

import com.ch.Constants;
import com.ch.cloud.gateway.conf.CookieConfig;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.utils.UserAuthUtils;
import com.ch.cloud.sso.pojo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
            RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.key(md5),
                    JsonJacksonCodec.INSTANCE);
            if (!userBucket.isExists()) {
                return false;
            }

            UserInfo user = userBucket.get();
            if (user == null) {
                return false;
            }

            // 计算剩余时间
            long currentTime = System.currentTimeMillis();
            long expireTime = user.getExpireAt();
            long timeToExpire = expireTime - currentTime;

            // 如果token在阈值时间内过期，则需要刷新
            boolean needRefresh = timeToExpire <= cookieConfig.getRefreshThreshold() * 1000L;

            if (needRefresh && cookieConfig.isEnableLog()) {
                log.debug("Cookie即将过期，剩余时间: {}秒", timeToExpire / 1000);
            }
            if (needRefresh && UserAuthUtils.renewToken(token)) {
                // 更新Redis中的用户信息 续期30分钟
                Duration duration = Duration.of(currentTime + cookieConfig.getMaxAge(), ChronoUnit.MILLIS);
                userBucket.expire(duration);
                return true;
            }
        } catch (Exception e) {
            log.error("检查Cookie刷新状态时发生错误", e);
        }
        return false;
    }

    /**
     * 刷新Cookie
     *
     * @param response HTTP响应对象
     * @param token    用户token
     */
    public void refreshCookie(ServerHttpResponse response, String token) {
        try {
            // 创建新的Cookie
            ResponseCookie newCookie = ResponseCookie.from(cookieConfig.getTokenName(), token)
                    .maxAge(cookieConfig.getMaxAge()).path(cookieConfig.getPath()).httpOnly(cookieConfig.isHttpOnly())
                    .secure(cookieConfig.isSecure()).build();

            // 添加Cookie到响应头
            response.addCookie(newCookie);

            if (cookieConfig.isEnableLog()) {
                log.debug("Cookie已刷新，名称: {}, 过期时间: {}秒", cookieConfig.getTokenName(),
                        cookieConfig.getMaxAge());
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

            ResponseCookie newCookie = ResponseCookie.from(cookieConfig.getTokenName(), "").maxAge(0)// 立即过期
                    .path(cookieConfig.getPath()).httpOnly(cookieConfig.isHttpOnly()).secure(cookieConfig.isSecure())
                    .build();
            response.addCookie(newCookie);
            ResponseCookie newCookie2 = ResponseCookie.from(Constants.X_REFRESH_TOKEN, "").maxAge(0)// 立即过期
                    .path(cookieConfig.getPath()).httpOnly(cookieConfig.isHttpOnly()).secure(cookieConfig.isSecure())
                    .build();
            response.addCookie(newCookie2);

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

    public String refreshToken(String token, String refreshToken) {

        if (!cookieConfig.isAutoRefresh()) {
            return null;
        }

        return UserAuthUtils.refreshToken(token,refreshToken);
    }
}
