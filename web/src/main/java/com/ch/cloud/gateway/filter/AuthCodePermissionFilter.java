package com.ch.cloud.gateway.filter;

import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.utils.UserAuthUtils;
import com.ch.cloud.upms.dto.AuthCodePermissionDTO;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 授权码校验过滤器 从URL参数获取token，校验通过则放行
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class AuthCodePermissionFilter extends AbstractPermissionFilter {
    
    @Override
    protected int getFilterOrder() {
        return -190; // 在Cookie和白名单之间
    }
    
    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tempToken = request.getQueryParams().getFirst("apiKey");
        String path = request.getURI().getPath();
        log.debug("授权码校验: {}，apiKey={}", path, tempToken);
        
        if (CommonUtils.isEmpty(tempToken)) {
            log.warn("授权码缺失，路径: {}，必须提供apiKey参数", path);
            return UserAuthUtils.authError(exchange.getResponse(),
                    Result.error(PubError.INVALID, "缺少临时授权码apiKey参数"));
        }
        // 校验授权码（通过feignClientHolder获取DTO并校验）
        try {
            AuthCodePermissionDTO dto = UserAuthUtils.getAuthCodeInfo(tempToken);
            if (dto == null) {
                log.warn("授权码不存在: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(), Result.error(PubError.INVALID, "授权码不存在"));
            }
            if (dto.getStatus() == null || dto.getStatus() != 1) {
                log.warn("授权码状态无效: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "授权码状态无效"));
            }
            if (dto.getExpireTime() != null && dto.getExpireTime().before(new java.util.Date())) {
                log.warn("授权码已过期: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(), Result.error(PubError.INVALID, "授权码已过期"));
            }
            if (dto.getMaxUses() != null && dto.getUsedCount() != null && dto.getUsedCount() >= dto.getMaxUses()) {
                log.warn("授权码已超出最大使用次数: {}", tempToken);
                return UserAuthUtils.authError(exchange.getResponse(),
                        Result.error(PubError.INVALID, "授权码已超出最大使用次数"));
            }
            // 新增：校验权限
            if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
                boolean allowed = checkPermissions(dto.getPermissions(), path, request.getMethod());
                if (!allowed) {
                    log.warn("授权码权限不足: {}，path: {}", tempToken, path);
                    return UserAuthUtils.authError(exchange.getResponse(),
                            Result.error(PubError.NOT_AUTH, "授权码无权访问该接口"));
                }
            }
            // 其他权限校验可根据业务补充
            log.info("授权码校验通过，路径: {}，token: {}", path, tempToken);
            return skipAfterFilter(exchange, chain);
        } catch (Exception e) {
            log.error("授权码校验异常", e);
            return UserAuthUtils.authError(exchange.getResponse(), Result.error(PubError.INVALID, "授权码校验异常"));
        }
    }
    
    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 如果已经被白名单，则跳过
        return true;
    }
    
    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Collection<PermissionDto> permissions = getPermissions(CacheType.PERMISSIONS_TEMP_LIST, null);
        
        if (!permissions.isEmpty()) {
            boolean isTempList = checkPermissions(permissions, path, exchange.getRequest().getMethod());
            if (isTempList) {
                log.debug("路径 {} 需要临时授权码验证", path);
                return true;
            }
        }
        return false;
    }
    
} 