package com.ch.cloud.gateway.service;

import com.ch.cloud.sso.client.SsoLoginClient;
import com.ch.cloud.sso.client.SsoUserClient;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.client.UpmsAuthCodeClient;
import com.ch.cloud.upms.client.UpmsPermissionClient;
import com.ch.cloud.upms.client.UpmsRoleClient;
import com.ch.cloud.upms.dto.AuthCodePermissionDTO;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

/**
 * 描述：
 *
 * @author Zhimin.Ma
 * @since 2022/5/25
 */
@Slf4j
@Component
public class FeignClientHolder {
    
    @Lazy // 重点：这里必须使用@Lazy(异步线程)
    @Autowired
    private SsoLoginClient ssoLoginClient;
    
    @Lazy // 重点：这里必须使用@Lazy(异步线程)
    @Autowired
    private SsoUserClient ssoUserClient;
    
    @Lazy
    @Autowired
    private UpmsPermissionClient upmsPermissionClient;
    
    @Lazy
    @Autowired
    private UpmsRoleClient upmsRoleClient;
    
    @Lazy
    @Autowired
    private UpmsAuthCodeClient upmsAuthCodeClient;
    
    /**
     * 这里必须在异步线程中执行，执行结果返回Future
     *
     * @param token 登录Token
     * @return 登录用户信息
     */
    @Async
    public Future<Result<UserInfo>> tokenInfo(String token) {
        log.info("开始获取 user info ...");
        Result<UserInfo> r1 = ssoLoginClient.info(token);
        if (!r1.isEmpty()) {
            Result<UserInfo> r2 = ssoUserClient.info(r1.get().getUsername());
            if (!r2.isEmpty()) {
                r1.get().setUserId(r2.get().getUserId());
                r1.get().setRoleId(r2.get().getRoleId());
                r1.get().setTenantId(r2.get().getTenantId());
            } else {
                log.error("获取 user info 错误！{}", r2.getMessage());
                r1.setCode(r2.getCode());
                r1.setMessage(r2.getMessage());
            }
        } else {
            log.error("获取 login info 失败！{}", r1.getMessage());
        }
        return new AsyncResult<>(r1);
    }
    
    @Async
    public Future<Boolean> tokenRenew(String token) {
        log.info("开始使用 tokenRenew ...");
        Result<Boolean> renewResult = ssoLoginClient.renew(token);
        return new AsyncResult<>(renewResult.get());
    }
    
    @Async
    public Future<AuthCodePermissionDTO> authCodePermissions(String code) {
        Result<AuthCodePermissionDTO> res = upmsAuthCodeClient.getPermission(code);
        return new AsyncResult<>(res.get());
    }
    
    @Async
    public Future<Result<PermissionDto>> whitelistPermissions() {
        return new AsyncResult<>(upmsPermissionClient.whitelist());
    }
    
    @Async
    public Future<Result<PermissionDto>> hiddenPermissions() {
        return new AsyncResult<>(upmsPermissionClient.hidden());
    }
    
    @Async
    public Future<Result<PermissionDto>> cookiePermissions() {
        return new AsyncResult<>(upmsPermissionClient.cookie());
    }
    
    @Async
    public Future<Result<PermissionDto>> rolePermissions(Long roleId) {
        return new AsyncResult<>(upmsRoleClient.findPermissionsByRoleId(roleId, null));
    }
    
    @Async
    public Future<Result<PermissionDto>> tempPermissions() {
        return new AsyncResult<>(upmsPermissionClient.authCode());
    }
}
