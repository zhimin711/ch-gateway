package com.ch.cloud.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.ch.Constants;
import com.ch.StatusS;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.service.FeignClientHolder;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import com.ch.utils.EncryptUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限过滤器 - 已废弃
 * 
 * 该类已被重构为多个独立的权限过滤器：
 * - WhiteListPermissionFilter: 白名单权限过滤器
 * - CookiePermissionFilter: Cookie权限过滤器  
 * - LoginPermissionFilter: 登录权限过滤器
 * - RolePermissionFilter: 角色权限过滤器
 * 
 * 请使用新的权限过滤器架构
 *
 * @author zhimi
 * @since 2020-1-1
 * @deprecated 已废弃，请使用新的权限过滤器架构
 */
@Configuration
@Slf4j
@Deprecated
public class JwtAuthenticationTokenFilter implements GlobalFilter, Ordered {
    
    private final String[] skipUrls = {"/auth/captcha/**", "/auth/login/**", "/auth/logout/**", "/*/static/**"};
    
    private final String[] authUrls = {"/auth/user/**"};
    
    @Autowired
    private FeignClientHolder feignClientHolder;
    
    @Resource
    private RedissonClient redissonClient;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 已废弃的过滤器，直接放行，由新的权限过滤器处理
        log.warn("使用已废弃的JwtAuthenticationTokenFilter，建议迁移到新的权限过滤器架构");
        return chain.filter(exchange);
    }
    
    private Result<UserInfo> getUserInfo(String token) {
        String md5 = EncryptUtils.md5(token);
        RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(md5),
                JsonJacksonCodec.INSTANCE);
        Result<UserInfo> infoResult = Result.failed();
        if (!userBucket.isExists()) {
            try {
                Future<Result<UserInfo>> f = feignClientHolder.tokenInfo(token);
                infoResult = f.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("[单点登录系统]调用登录鉴权Feign失败", e);
                infoResult.setCode("100");
                infoResult.setMessage("[单点登录]Feign调用登录鉴权失败");
            }
            if (!infoResult.isSuccess()) {
                return infoResult;
            }
            
            UserInfo user = infoResult.get();
            RBucket<String> tokenBucket = redissonClient.getBucket(
                    CacheType.GATEWAY_USER.getKey(infoResult.get().getUsername()));
            if (tokenBucket.isExists()) {
                redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(tokenBucket.get())).delete();
            }
            tokenBucket.set(md5);
            userBucket.set(user, user.getExpireAt(), TimeUnit.MICROSECONDS);
        } else {
            try {
                return Result.success(userBucket.get());
            } catch (Exception e) {
                log.error("read user cache error!", e);
                return Result.error(PubError.INVALID,"网关解析Token信息缓存错误");
            }
        }
        return infoResult;
    }
    
    private Collection<PermissionDto> getPermissions2(CacheType cacheType, Long roleId) {
        RMapCache<String, List<PermissionDto>> permissionsMap = redissonClient.getMapCache(
                CacheType.PERMISSIONS_MAP.getKey(), JsonJacksonCodec.INSTANCE);
        String key = roleId != null ? roleId.toString() : cacheType.getCode();
        List<PermissionDto> permissions = permissionsMap.get(key);
        if (permissions == null) {
            if (cacheType == CacheType.PERMISSIONS_AUTH_LIST && roleId == null) {
                return Lists.newArrayList();
            }
            Collection<PermissionDto> list = getPermissions3(cacheType, roleId);
            permissionsMap.fastPutIfAbsent(key, Lists.newArrayList(list), 30, TimeUnit.MINUTES);
            return list;
        }
        return permissions;
    }
    
    
    private Collection<PermissionDto> getPermissions3(CacheType cacheType, Long roleId) {
        Result<PermissionDto> res = Result.success();
        try {
            switch (cacheType) {
                case PERMISSIONS_WHITE_LIST:
                    res = feignClientHolder.whitelistPermissions().get();
                    break;
                case PERMISSIONS_LOGIN_LIST:
                    res = feignClientHolder.hiddenPermissions().get();
                    break;
                case PERMISSIONS_COOKIE_LIST:
                    res = feignClientHolder.cookiePermissions().get();
                    break;
                default:
                    res = feignClientHolder.rolePermissions(roleId).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("[用户权限]调用用户权限中心Feign失败", e);
        }
        return res.getRows();
    }
    
    private String getCookieToken(ServerHttpRequest request) {
        Collection<PermissionDto> permissions = getPermissions2(CacheType.PERMISSIONS_COOKIE_LIST, null);
        boolean ok = checkPermissions(permissions, request.getURI().getPath(), request.getMethod());
        if (!ok) {
            return null;
        }
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        HttpCookie cookie = cookies.getFirst("TOKEN");
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }
    
    private Mono<Void> toUser(ServerWebExchange exchange, GatewayFilterChain chain, UserInfo user) {
        
        ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                .header(Constants.CURRENT_USER, user.getUserId())
                .header(Constants.X_TOKEN_USER, user.getUsername())
                .header(Constants.X_TOKEN_TENANT, user.getTenantId() == null ? "" : user.getTenantId().toString())
                .build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

        return chain.filter(mutableExchange);
    }
    
    private boolean checkPermissions(Collection<PermissionDto> permissions, String path, HttpMethod method) {
        if (permissions.isEmpty()) {
            return false;
        }
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        Map<String, List<PermissionDto>> permissionMap = permissions.stream()
                .collect(Collectors.groupingBy(PermissionDto::getUrl));
        for (Map.Entry<String, List<PermissionDto>> entry : permissionMap.entrySet()) {
            String url = entry.getKey();
            boolean ok = pathMatcher.match(url, path);
            if (ok) {
                for (PermissionDto dto : permissionMap.get(url)) {
                    if (CommonUtils.isEmpty(dto.getMethod()) || method.matches(dto.getMethod())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean checkSkinUrl(String url) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        boolean isSkin = false;
        for (String authUrl : skipUrls) {
            if (pathMatcher.match(authUrl, url)) {
                isSkin = true;
                break;
            }
        }
        return isSkin;
    }
    
    private boolean checkAuthUrl(String url) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        boolean isNeed = false;
        for (String authUrl : authUrls) {
            if (pathMatcher.match(authUrl, url)) {
                isNeed = true;
                break;
            }
        }
        return isNeed;
    }
    
    
    private void refreshToken(ServerHttpResponse originalResponse, String refreshToken) {
//        originalResponse.setStatusCode(HttpStatus.OK);
        //token过期设置刷新标识
        originalResponse.getHeaders().add(Constants.X_REFRESH_TOKEN, refreshToken);
    }
    
    /**
     * 认证错误输出
     *
     * @param resp   响应对象
     * @param result 错误信息
     * @return
     */
    private Mono<Void> authError(ServerHttpResponse resp, Result<?> result) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
//        Result<Object> res = Result.error(PubError.NOT_AUTH, mess);
        String returnStr = JSON.toJSONString(result);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }
    
    
    @Override
    public int getOrder() {
        return -50; // 降低优先级，让新的过滤器先执行
    }
}
