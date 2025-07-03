package com.ch.cloud.gateway.utils;

import com.alibaba.fastjson2.JSON;
import com.ch.Constants;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.service.FeignClientHolder;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.dto.AuthCodePermissionDTO;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.EncryptUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 用户认证工具类 提供通用的用户信息获取和错误处理功能
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Slf4j
public class UserAuthUtils {
    
    @Setter
    private static FeignClientHolder feignClientHolder;
    
    @Setter
    private static RedissonClient redissonClient;
    
    /**
     * 获取用户信息
     */
    public static Result<UserInfo> getUserInfo(String token) {
        String md5 = EncryptUtils.md5(token);
        RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.key(md5),
                JsonJacksonCodec.INSTANCE);
        Result<UserInfo> userResult = Result.failed();
        
        if (!userBucket.isExists()) {
            try {
                Future<Result<UserInfo>> f = feignClientHolder.tokenInfo(token);
                userResult = f.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("[单点登录系统]调用登录鉴权Feign失败", e);
                userResult.setCode("100");
                userResult.setMessage("[单点登录]Feign调用登录鉴权失败");
            }
            
            if (!userResult.isSuccess()) {
                return userResult;
            }
            
            UserInfo user = userResult.get();
            RBucket<String> tokenBucket = redissonClient.getBucket(
                    CacheType.GATEWAY_USER.key(user.getUsername()));
            if (tokenBucket.isExists()) {
                redissonClient.getBucket(CacheType.GATEWAY_TOKEN.key(tokenBucket.get())).delete();
            }
            tokenBucket.set(md5);
            Duration duration = Duration.of(user.getExpireAt() - System.currentTimeMillis(), ChronoUnit.MILLIS);
            userBucket.set(user, duration);
        } else {
            try {
                return Result.success(userBucket.get());
            } catch (Exception e) {
                log.error("read user cache error!", e);
                return Result.error(PubError.INVALID, "网关解析Token信息缓存错误");
            }
        }
        return userResult;
    }
    
    /**
     * 获取授权码信息
     */
    public static AuthCodePermissionDTO getAuthCodeInfo(String code) {
        
        try {
            Future<AuthCodePermissionDTO> future = feignClientHolder.authCodePermissions(code);
            return future.get();
        } catch (Exception e) {
            log.error("[用户权限系统]调用授权码鉴权Feign失败", e);
            
        }
        return null;
    }
    
    /**
     * 将用户信息添加到请求头
     */
    public static Mono<Void> toUser(ServerWebExchange exchange, GatewayFilterChain chain, UserInfo user,
            boolean skipAfter) {
        ServerHttpRequest mutableReq = exchange.getRequest().mutate().header(Constants.CURRENT_USER, user.getUserId())
                .header(Constants.X_TOKEN_USER, user.getUsername())
                .header(Constants.X_TOKEN_TENANT, user.getTenantId() == null ? "" : user.getTenantId().toString())
                .header(GatewayConstants.FILTER_HEADER_SKIP_AFTER, skipAfter ? "true" : "false").build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
        return chain.filter(mutableExchange);
    }
    
    /**
     * 刷新token
     */
    public static void refreshToken(ServerHttpResponse originalResponse, String refreshToken) {
        originalResponse.getHeaders().add(Constants.X_REFRESH_TOKEN, refreshToken);
    }
    
    /**
     * 认证错误输出
     */
    public static Mono<Void> authError(ServerHttpResponse resp, Result<?> result) {
//        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String returnStr = JSON.toJSONString(result);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }
} 