package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.utils.UserAuthUtils;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.e.Error;
import com.ch.e.PubError;
import com.ch.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 角色权限过滤器 处理需要角色权限验证的路径
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class RolePermissionFilter extends AbstractPermissionFilter {
    
    @Override
    protected int getFilterOrder() {
        return -100; // 第三优先级
    }
    
    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("角色权限检查: {}", path);
        
        // 获取token
        String token = exchange.getRequest().getHeaders().getFirst(Constants.X_TOKEN);
        
        // 验证token并获取用户信息
        Result<UserInfo> userResult = UserAuthUtils.getUserInfo(token);
        
        UserInfo user = userResult.get();
        log.debug("用户信息获取成功: {}", user.getUsername());
        
        // 检查角色权限
        Collection<PermissionDto> authPermissions = getPermissions(CacheType.PERMISSIONS_AUTH_LIST, user.getRoleId());
        boolean hasPermission = checkPermissions(authPermissions, path, exchange.getRequest().getMethod());
        
        if (!hasPermission) {
            log.warn("用户 {} 没有访问路径 {} 的权限", user.getUsername(), path);
            return UserAuthUtils.authError(exchange.getResponse(),
                    Result.error(Error.buildWithArgs(PubError.NOT_AUTH, "角色ID:" + user.getRoleId(), path)));
        }
        
        log.debug("角色权限验证通过，用户: {}, 角色: {}", user.getUsername(), user.getRoleId());
        
        // 将用户信息添加到请求头
        return chain.filter(exchange);
    }
    
    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 如果已经被白名单或授权码处理，则跳过
        return true;
    }
    
    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        //        String path = exchange.getRequest().getURI().getPath();
        
        // 其他路径都需要角色权限验证
        //        log.debug("路径 {} 需要角色权限验证", path);
        return true;
    }
} 