package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.StatusS;
import com.ch.cloud.client.dto.PermissionDto;
import com.ch.cloud.gateway.cli.SsoClientService;
import com.ch.cloud.gateway.cli.UpmsClientService;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import com.ch.utils.JSONUtils;
import com.google.common.collect.Lists;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class JwtAuthenticationTokenFilter implements GlobalFilter, Ordered {

    private String[] skipUrls = {"/auth/login/**", "/auth/logout/**"};
    private String[] authUrls = {"/auth/login/token/user"};

    private Collection<PermissionDto> skipPermissions;

    @Resource
    private SsoClientService ssoClientService;
    @Resource
    private UpmsClientService upmsClientService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        if (checkSkinUrl(url) && !checkAuthUrl(url)) {
            //跳过不需要验证的路径
            return chain.filter(exchange);
        }
        //获取token
        String token = exchange.getRequest().getHeaders().getFirst(Constants.TOKEN_HEADER2);
        ServerHttpResponse resp = exchange.getResponse();
        if (CommonUtils.isEmpty(token)) {
            //没有token
            return authError(resp, Result.error(PubError.NOT_LOGIN, "未登录，请先登陆..."));
        } else {
            //有token
            Result<UserInfo> res = ssoClientService.tokenInfo(token);
            if (res.isEmpty()) {
                PubError err = PubError.fromCode(res.getCode());
                if (err == PubError.EXPIRED) {
                    refreshToken(resp, StatusS.ENABLED);
                }

                return authError(resp, Result.error(err, res.getMessage()));
            }
            if (skipPermissions == null) {
                Result<PermissionDto> res3 = upmsClientService.findHiddenPermissions();
                skipPermissions = res3.isEmpty() ? Lists.newArrayList() : res3.getRows();
            }
            if (!skipPermissions.isEmpty()) {
                boolean ok = checkPermissions(skipPermissions, exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
                if (ok) {
                    //将现在的request，添加当前身份
                    return toUser(exchange, chain, res.get().getUsername());
                }
            }

            Result<PermissionDto> res2 = upmsClientService.findPermissionsByRoleId(res.get().getRoleId());
            if (res2.isEmpty()) {
                return authError(resp, Result.error(PubError.NOT_AUTH));
            }
            boolean ok = checkPermissions(res2.getRows(), exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
            if (!ok) {
                return authError(resp, Result.error(PubError.NOT_AUTH));
            }
            //将现在的request，添加当前身份
            return toUser(exchange, chain, res.get().getUsername());
        }
//        return null;
    }

    private Mono<Void> toUser(ServerWebExchange exchange, GatewayFilterChain chain, String username) {
        ServerHttpRequest mutableReq = exchange.getRequest().mutate().header(Constants.TOKEN_USER, username).build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
        return chain.filter(mutableExchange);
    }

    private boolean checkPermissions(Collection<PermissionDto> permissions, String path, HttpMethod method) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        Map<String, List<PermissionDto>> permissionMap = permissions.stream().collect(Collectors.groupingBy(PermissionDto::getUrl));
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
//                isNeed = true;
                break;
            }
        }
        return isNeed;
    }


    private void refreshToken(ServerHttpResponse originalResponse, String refreshToken) {
//        originalResponse.setStatusCode(HttpStatus.OK);
        //token过期设置刷新标识
        originalResponse.getHeaders().add("X-TOKEN-REFRESH", refreshToken);
    }

    /**
     * 认证错误输出
     *
     * @param resp   响应对象
     * @param result 错误信息
     * @return
     */
    private Mono<Void> authError(ServerHttpResponse resp, Result result) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
//        Result<Object> res = Result.error(PubError.NOT_AUTH, mess);
        String returnStr = JSONUtils.toJson(result);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }


    @Override
    public int getOrder() {
        return -100;
    }
}
