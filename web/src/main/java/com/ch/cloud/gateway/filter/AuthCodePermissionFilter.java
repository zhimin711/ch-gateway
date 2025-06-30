package com.ch.cloud.gateway.filter;

import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.upms.dto.AuthCodePermissionDTO;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 临时码校验过滤器 从URL参数获取token，校验通过则放行
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class AuthCodePermissionFilter extends AbstractPermissionFilter {
    
    @Resource
    private RedissonClient redissonClient;
    
    @Override
    protected int getFilterOrder() {
        return -190; // 在Cookie和白名单之间
    }
    
    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tempToken = request.getQueryParams().getFirst("token");
        String path = request.getURI().getPath();
        log.debug("临时码校验: {}，token={}", path, tempToken);
        
        if (CommonUtils.isEmpty(tempToken)) {
            log.warn("临时码缺失，路径: {}，必须提供token参数", path);
            return UserAuthUtils.authError(exchange.getResponse(),
                    Result.error(PubError.INVALID, "缺少临时校验码token参数"));
        }
        
        // 校验临时码（通过feignClientHolder获取DTO并校验）
        try {
            Future<AuthCodePermissionDTO> future = feignClientHolder.authCodePermissions(tempToken);
            AuthCodePermissionDTO dto = future.get();
            if (dto == null) {
                log.warn("临时码不存在: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "临时码不存在"));
            }
            if (dto.getStatus() == null || dto.getStatus() != 1) {
                log.warn("临时码状态无效: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "临时码状态无效"));
            }
            if (dto.getExpireTime() != null && dto.getExpireTime().before(new java.util.Date())) {
                log.warn("临时码已过期: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "临时码已过期"));
            }
            if (dto.getMaxUses() != null && dto.getUsedCount() != null && dto.getUsedCount() >= dto.getMaxUses()) {
                log.warn("临时码已超出最大使用次数: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "临时码已超出最大使用次数"));
            }
            // 新增：校验权限
            if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
                boolean allowed = checkPermissions(dto.getPermissions(), path, request.getMethod());
                if (!allowed) {
                    log.warn("临时码权限不足: {}，path: {}", tempToken, path);
                    return UserAuthUtils.authError(exchange.getResponse(),
                            Result.error(PubError.NOT_AUTH, "临时码无权访问该接口"));
                }
            }
            // 其他权限校验可根据业务补充
            log.info("临时码校验通过，路径: {}，token: {}", path, tempToken);
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("临时码校验异常", e);
            return UserAuthUtils.authError(exchange.getResponse(),
                    Result.error(PubError.INVALID, "临时码校验异常"));
        }
    }
    
    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 如果URL中有token参数，则跳过后续过滤器（让当前过滤器处理）
        String tempToken = exchange.getRequest().getQueryParams().getFirst("token");
        if (CommonUtils.isNotEmpty(tempToken)) {
            log.debug("检测到临时校验码，跳过后续过滤器");
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Collection<PermissionDto> permissions = getPermissions(CacheType.PERMISSIONS_TEMP_LIST, null);
        
        if (!permissions.isEmpty()) {
            boolean isTempSupported = checkPermissions(permissions, path, exchange.getRequest().getMethod());
            if (isTempSupported) {
                log.debug("路径 {} 支持temporary auth code", path);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 校验临时码逻辑（可根据实际业务实现）
     */
    private boolean validateTempToken(String tempToken) {
        // 示例：假设Redis中存在key为TEMP_CODE:{tempToken}即为有效
        String key = "TEMP_CODE:" + tempToken;
        Future<AuthCodePermissionDTO> future = feignClientHolder.authCodePermissions(tempToken);
        try {
            AuthCodePermissionDTO dto = future.get();
            
        } catch (Exception e) {
            log.error("临时码校验异常", e);
        }
        return false;
    }
} 