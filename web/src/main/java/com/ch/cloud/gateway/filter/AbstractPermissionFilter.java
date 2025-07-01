package com.ch.cloud.gateway.filter;

import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.service.FeignClientHolder;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.cloud.upms.enums.PermissionType;
import com.ch.result.Result;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限过滤器抽象基类
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Slf4j
public abstract class AbstractPermissionFilter implements GlobalFilter, Ordered {
    
    @Autowired
    protected FeignClientHolder feignClientHolder;
    
    @Resource
    protected RedissonClient redissonClient;
    
    /**
     * 获取过滤器优先级
     */
    @Override
    public int getOrder() {
        return getFilterOrder();
    }
    
    /**
     * 子类实现具体的过滤器逻辑
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange);
        }
        
        if (shouldProcess(exchange)) {
            return doFilter(exchange, chain);
        }
        
        return chain.filter(exchange);
    }
    
    /**
     * 子类需要实现的过滤器优先级
     */
    protected abstract int getFilterOrder();
    
    /**
     * 子类需要实现的过滤器逻辑
     */
    protected abstract Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain);
    
    /**
     * 子类需要实现的跳过条件
     */
    protected abstract boolean shouldSkip(ServerWebExchange exchange);
    
    /**
     * 子类需要实现的处理条件
     */
    protected abstract boolean shouldProcess(ServerWebExchange exchange);
    
    /**
     * 获取权限列表
     */
    protected Collection<PermissionDto> getPermissions(CacheType cacheType, Long roleId) {
        RMapCache<String, List<PermissionDto>> permissionsMap = redissonClient.getMapCache(
                CacheType.PERMISSIONS_MAP.getKey(), JsonJacksonCodec.INSTANCE);
        String key = roleId != null ? roleId.toString() : cacheType.getCode();
        List<PermissionDto> permissions = permissionsMap.get(key);
        
        if (permissions == null) {
            if (cacheType == CacheType.PERMISSIONS_AUTH_LIST && roleId == null) {
                return Lists.newArrayList();
            }
            Collection<PermissionDto> list = fetchPermissions(cacheType, roleId);
            if (cacheType == CacheType.PERMISSIONS_AUTH_LIST) {
                // 过滤掉权限码接口
                list = list.stream().filter(dto -> PermissionType.from(dto.getType(), dto.getHidden())
                        != PermissionType.AUTH_CODE_INTERFACE).collect(Collectors.toList());
            }
            permissionsMap.fastPutIfAbsent(key, Lists.newArrayList(list), 30, TimeUnit.MINUTES);
            return list;
        }
        return permissions;
    }
    
    /**
     * 从远程服务获取权限列表
     */
    protected Collection<PermissionDto> fetchPermissions(CacheType cacheType, Long roleId) {
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
                case PERMISSIONS_TEMP_LIST:
                    res = feignClientHolder.tempPermissions().get();
                    break;
                default:
                    res = feignClientHolder.rolePermissions(roleId).get();
                    break;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("[用户权限]调用用户权限中心Feign失败", e);
        }
        return res.getRows();
    }
    
    /**
     * 检查权限
     */
    protected boolean checkPermissions(Collection<PermissionDto> permissions, String path, HttpMethod method) {
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
                    if (dto.getMethod() == null || method.matches(dto.getMethod())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
} 