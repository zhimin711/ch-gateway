package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.StatusS;
import com.ch.cloud.client.dto.PermissionDto;
import com.ch.cloud.gateway.cli.SsoClientService;
import com.ch.cloud.gateway.cli.UpmsClientService;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.cloud.gateway.utils.PathConstants;
import com.ch.e.PubError;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import com.ch.utils.EncryptUtils;
import com.ch.utils.JSONUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限过滤器
 *
 * @author zhimi
 * @since 2020-1-1
 */
@Configuration
public class JwtAuthenticationTokenFilter implements GlobalFilter, Ordered {

    private final String[] skipUrls = {"/auth/captcha/**", "/auth/login/**", "/auth/logout/**"};
    private final String[] authUrls = {"/auth/user/**"};

    @Resource
    private SsoClientService ssoClientService;
    @Resource
    private UpmsClientService upmsClientService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        if (checkSkinUrl(url)) {
            //跳过不需要验证的路径
            return chain.filter(exchange);
        }
        Collection<PermissionDto> whiteList = getPermissions(CacheType.PERMISSIONS_WHITE_LIST, null);
        if (!whiteList.isEmpty()) { // 白名单接口地址
            boolean ok = checkPermissions(whiteList, exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
            if (ok) {
                //将现在的request，添加当前身份
                return chain.filter(exchange);
            }
        }
        //获取token
        String token = exchange.getRequest().getHeaders().getFirst(Constants.X_TOKEN);
        ServerHttpResponse resp = exchange.getResponse();
        if (checkAuthUrl(url)) {
            if (CommonUtils.isEmpty(token)) {
                return authError(resp, Result.error(PubError.NOT_LOGIN, "未登录，请先登陆..."));
            }
            return chain.filter(exchange);
        }
        if (CommonUtils.isEmpty(token)) {
            token = getCookieToken(exchange.getRequest());
        }
        if (CommonUtils.isEmpty(token)) {
            //没有token
            return authError(resp, Result.error(PubError.NOT_LOGIN, "未登录，请先登陆..."));
        } else {
            //有token
            //redis cache replace sso client
            String md5 = EncryptUtils.md5(token);
            RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(md5), JsonJacksonCodec.INSTANCE);
            if (!userBucket.isExists()) {
                Result<UserInfo> res1 = ssoClientService.tokenInfo(token);
                Result<UserInfo> res2 = ssoClientService.userInfo(token);
                if (res1.isEmpty() || res2.isEmpty()) {
                    PubError err = PubError.fromCode(res1.getCode());
                    if (err == PubError.EXPIRED) {
                        refreshToken(resp, StatusS.ENABLED);
                    }
                    return authError(resp, Result.error(err, res1.getMessage()));
                }
                UserInfo user = res1.get();
                user.setUserId(res2.get().getUserId());
                user.setRoleId(res2.get().getRoleId());
//                user.setRoleId(res2.get().getRoleId());

                RBucket<String> tokenBucket = redissonClient.getBucket(CacheType.GATEWAY_USER.getKey(res1.get().getUsername()));
                if (tokenBucket.isExists()) {
                    redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(tokenBucket.get())).delete();
                }
                tokenBucket.set(md5);
                userBucket.set(user, user.getExpireAt(), TimeUnit.MICROSECONDS);
            }

            //redis cache replace sso client findHiddenPermissions
            Collection<PermissionDto> hiddenPermissions = getPermissions(CacheType.PERMISSIONS_LOGIN_LIST, null);

            if (!hiddenPermissions.isEmpty()) {
                boolean ok = checkPermissions(hiddenPermissions, exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
                if (ok) {
                    //将现在的request，添加当前身份
                    return toUser(exchange, chain, userBucket.get().getUsername());
                }
            }

            //redis cache replace sso client findPermissionsByRoleId
            Collection<PermissionDto> authPermissions = getPermissions(CacheType.PERMISSIONS_AUTH_LIST, userBucket.get().getRoleId());

            boolean ok = checkPermissions(authPermissions, exchange.getRequest().getURI().getPath(), exchange.getRequest().getMethod());
            if (!ok) {
                return authError(resp, Result.error(PubError.NOT_AUTH, url + " not authority!"));
            }
            //将现在的request，添加当前身份
            return toUser(exchange, chain, userBucket.get().getUsername());
        }
//        return null;
    }

    private Collection<PermissionDto> getPermissions(CacheType cacheType, Long roleId) {

        RList<PermissionDto> permissions = redissonClient.getList(roleId == null ? cacheType.getKey() : cacheType.getKey(roleId.toString()), JsonJacksonCodec.INSTANCE);
        if (permissions.isEmpty()) {
            if (cacheType == CacheType.PERMISSIONS_AUTH_LIST && roleId == null) {
                return permissions;
            }
            if (cachePermissions(cacheType, roleId, permissions)) return permissions;
        }
        return permissions;
    }

    private synchronized boolean cachePermissions(CacheType cacheType, Long roleId, RList<PermissionDto> permissions) {
        Result<PermissionDto> res;
        switch (cacheType) {
            case PERMISSIONS_WHITE_LIST:
                res = upmsClientService.findWhitelistPermissions();
                break;
            case PERMISSIONS_LOGIN_LIST:
                res = upmsClientService.findHiddenPermissions();
                break;
            case PERMISSIONS_COOKIE_LIST:
                res = upmsClientService.findCookiePermissions();
                break;
            default:
                res = upmsClientService.findPermissionsByRoleId(roleId, null);
        }
        if (!permissions.isEmpty()) {
            return true;
        }
        if (!res.isEmpty()) {
            permissions.addAll(res.getRows());
        } else {
            permissions.addAll(Collections.emptyList());
        }
        permissions.expire(30, TimeUnit.MINUTES);
        return false;
    }

    private String getCookieToken(ServerHttpRequest request) {
        Collection<PermissionDto> permissions = getPermissions(CacheType.PERMISSIONS_COOKIE_LIST, null);
        boolean ok = checkPermissions(permissions, request.getURI().getPath(), request.getMethod());
        if (!ok) {
            return null;
        }
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        HttpCookie cookie = cookies.getFirst("TOKEN");
        if (cookie == null) return null;
        return cookie.getValue();
    }

    private Mono<Void> toUser(ServerWebExchange exchange, GatewayFilterChain chain, String username) {
        ServerHttpRequest mutableReq = exchange.getRequest().mutate().header(Constants.X_TOKEN_USER, username).build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
        return chain.filter(mutableExchange);
    }

    private boolean checkPermissions(Collection<PermissionDto> permissions, String path, HttpMethod method) {
        if (permissions.isEmpty()) return false;
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
        String returnStr = JSONUtils.toJson(result);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }


    @Override
    public int getOrder() {
        return -100;
    }
}
