package com.ch.cloud.gateway.service;

import com.ch.cloud.sso.client.SsoLoginClient;
import com.ch.cloud.sso.client.SsoUserClient;
import com.ch.cloud.sso.pojo.UserInfo;
import com.ch.cloud.upms.client.UpmsAuthCodeClient;
import com.ch.cloud.upms.client.UpmsPermissionClient;
import com.ch.cloud.upms.client.UpmsRoleClient;
import com.ch.cloud.upms.client.UpmsUserClient;
import com.ch.cloud.upms.dto.AuthCodePermissionDTO;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.cloud.upms.dto.RoleDto;
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
    private UpmsUserClient upmsUserClient;

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
        Result<UserInfo> loginResult = ssoLoginClient.info(token);
        if (!loginResult.isEmpty()) {
            Result<UserInfo> userResult = ssoUserClient.info(loginResult.get().getUsername());
            if (!userResult.isEmpty()) {
                loginResult.get().setUserId(userResult.get().getUserId());
                loginResult.get().setRoleId(userResult.get().getRoleId());
                loginResult.get().setTenantId(userResult.get().getTenantId());
            } else {
                log.error("获取 user info 错误！{}", userResult.getMessage());
                loginResult.setCode(userResult.getCode());
                loginResult.setMessage(userResult.getMessage());
            }
        } else {
            log.error("获取 login info 失败！{}", loginResult.getMessage());
        }
        return new AsyncResult<>(loginResult);
    }

    @Async
    public Future<Boolean> tokenValid(String token) {
        log.info("开始使用 tokenValid ...");
        Result<Boolean> validResult = ssoLoginClient.validate(token);
        return new AsyncResult<>(validResult.get());
    }

    @Async
    public Future<Boolean> tokenRenew(String token) {
        log.info("开始使用 tokenRenew ...");
        Result<Boolean> renewResult = ssoLoginClient.renew(token);
        return new AsyncResult<>(renewResult.get());
    }

    @Async
    public Future<String> refreshToken(String token,String refreshToken) {
        log.info("开始使用 refreshToken ...");
        Result<String> result = ssoLoginClient.refresh(token, refreshToken);
        return new AsyncResult<>(result.get());
    }

    @Async
    public Future<AuthCodePermissionDTO> authCodePermissions(String code) {
        Result<AuthCodePermissionDTO> res = upmsAuthCodeClient.getPermission(code);
        if (!res.isSuccess()) {
            log.info("授权码鉴权失败{}", res.getMessage());
        }
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

    @Async
    public Future<RoleDto> userRole(String username, Long roleId) {
        Result<RoleDto> result = upmsUserClient.findRolesByUsername(username);
        if (!result.isSuccess()) {
            log.error("获取用户角色失败{}", result.getMessage());
            return new AsyncResult<>(null);
        }
        RoleDto roleDto = result.getRows().stream().filter(role -> role.getId().equals(roleId)).findFirst().orElse(null);
        return new AsyncResult<>(roleDto);
    }
}
