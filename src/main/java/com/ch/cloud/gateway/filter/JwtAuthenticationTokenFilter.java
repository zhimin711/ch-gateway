package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.cloud.client.dto.PermissionDto;
import com.ch.cloud.gateway.cli.SsoClientService;
import com.ch.cloud.gateway.cli.UpmsClientService;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import com.ch.utils.JSONUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

@Configuration
public class JwtAuthenticationTokenFilter implements GlobalFilter, Ordered {

    private String[] skipAuthUrls;

    @Resource
    private SsoClientService ssoClientService;
    @Resource
    private UpmsClientService upmsClientService;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        //跳过不需要验证的路径
        if (null != skipAuthUrls && Arrays.asList(skipAuthUrls).contains(url)) {
            return chain.filter(exchange);
        }

        //获取token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        ServerHttpResponse resp = exchange.getResponse();
        if (CommonUtils.isEmpty(token)) {
            //没有token
            return authError(resp, "请登陆");
        } else {
            //有token

            Result<UserInfo> res = ssoClientService.tokenInfo(token);
            if (res.isEmpty()) {
                return authError(resp, "TOKEN失效");
            }
            Result<PermissionDto> res2 = upmsClientService.findPermissionsByRoleId(res.get().getRoleId());

            if (res2.isEmpty()) {
                return authError(resp, "未授权");
            }
            boolean ok = checkPermissions(res2.getRows(), exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
            if (!ok) {
                return authError(resp, "未授权");
            }
            //将现在的request，添加当前身份
            ServerHttpRequest mutableReq = exchange.getRequest().mutate().header(Constants.TOKEN_USER, res.get().getUsername()).build();
            ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
            return chain.filter(mutableExchange);
        }
//        return null;
    }

    private boolean checkPermissions(Collection<PermissionDto> permissions, String path, HttpMethod method) {
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        for (PermissionDto dto : permissions) {
            boolean ok = pathMatcher.match(dto.getUrl(), path);
            if (ok && (CommonUtils.isEmpty(dto.getMethod()) || method.matches(dto.getMethod()))) {
                return true;
            }
        }

        return false;
    }

    private Mono<Void> out1(ServerWebExchange exchange) {
        //未携带token或token在黑名单内
        ServerHttpResponse originalResponse = exchange.getResponse();
        originalResponse.setStatusCode(HttpStatus.OK);
        originalResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        byte[] response = "{\"code\": \"401\",\"msg\": \"401 Unauthorized.\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = originalResponse.bufferFactory().wrap(response);
        return originalResponse.writeWith(Flux.just(buffer));
    }

    /**
     * 认证错误输出
     *
     * @param resp 响应对象
     * @param mess 错误信息
     * @return
     */
    private Mono<Void> authError(ServerHttpResponse resp, String mess) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        Result<Object> res = Result.error(PubError.NOT_AUTH, mess);
        String returnStr = JSONUtils.toJson(res);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }


    @Override
    public int getOrder() {
        return -100;
    }
}
