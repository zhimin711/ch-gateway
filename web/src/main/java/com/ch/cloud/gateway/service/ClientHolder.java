package com.ch.cloud.gateway.service;

import com.ch.cloud.gateway.cli.SsoClientService;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.cloud.upms.client.UpmsPermissionClientService;
import com.ch.cloud.upms.client.UpmsRoleClientService;
import com.ch.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Future;

/**
 * 描述：
 *
 * @author Zhimin.Ma
 * @since 2022/5/25
 */
@Slf4j
@Component
public class ClientHolder {

    @Lazy // 重点：这里必须使用@Lazy
    @Autowired
    private SsoClientService ssoClientService;
    @Lazy // 重点：这里必须使用@Lazy
    @Autowired
    private UpmsPermissionClientService upmsPermissionClientService;
    @Lazy // 重点：这里必须使用@Lazy
    @Autowired
    private UpmsRoleClientService upmsRoleClientService;

    @Async// 重点：这里必须在异步线程中执行，执行结果返回Future
    public Future<UserInfo> tokenInfo(String token) {
        log.info("开始使用 tokenInfo ...");
        Result<UserInfo> s = ssoClientService.tokenInfo(token);
        return new AsyncResult<>(s.get());
    }

    @Async
    public Future<UserInfo> userInfo(String token) {
        log.info("开始使用 userInfo ...");
        Result<UserInfo> s = ssoClientService.userInfo(token);
        return new AsyncResult<>(s.get());
    }
}
