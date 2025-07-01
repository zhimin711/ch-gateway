package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.StatusS;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.utils.UserAuthUtils;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 登录权限过滤器
 * 处理只需要登录验证的路径
 *
 * @author zhimi
 * @since 2024-1-1
 */
@Configuration
@Slf4j
public class LoginPermissionFilter extends AbstractPermissionFilter {

    @Override
    protected int getFilterOrder() {
        return -150; // 第3 优先级
    }

    @Override
    protected Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("登录权限检查: {}", path);
        
        
        Collection<PermissionDto> loginPermissions = getPermissions(CacheType.PERMISSIONS_LOGIN_LIST, null);
        
        boolean skipAfter = false;
        if (!loginPermissions.isEmpty()) {
            boolean isLoginPermission = checkPermissions(loginPermissions, path, exchange.getRequest().getMethod());
            if (isLoginPermission) {
                log.debug("路径 {} 仅需要登录验证", path);
                skipAfter = true;
            }
        }
        
        // 获取token
        String token = exchange.getRequest().getHeaders().getFirst(Constants.X_TOKEN);
        ServerHttpResponse resp = exchange.getResponse();
        
        if (CommonUtils.isEmpty(token)) {
            return UserAuthUtils.authError(resp, Result.error(PubError.NOT_LOGIN, "未登录，请先登陆..."));
        }
        
        // 验证token并获取用户信息
        Result<UserInfo> userResult = UserAuthUtils.getUserInfo(token);
        if (CommonUtils.isNotEmpty(userResult.getCode()) && !userResult.isSuccess()) {
            PubError err = PubError.fromCode(userResult.getCode());
            if (err == PubError.EXPIRED) {
                UserAuthUtils.refreshToken(resp, StatusS.ENABLED);
            }
            return UserAuthUtils.authError(resp, Result.error(err, userResult.getMessage()));
        }
        
        UserInfo user = userResult.get();
        log.debug("登录权限验证通过，用户: {}", user.getUsername());
        
        // 将用户信息添加到请求头
        return UserAuthUtils.toUser(exchange, chain, user, skipAfter);
    }

    @Override
    protected boolean shouldSkip(ServerWebExchange exchange) {
        // 如果已经被白名单处理，则跳过
        return true;
    }

    @Override
    protected boolean shouldProcess(ServerWebExchange exchange) {
        
        return true;
    }
} 