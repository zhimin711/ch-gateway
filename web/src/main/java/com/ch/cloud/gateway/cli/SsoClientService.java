package com.ch.cloud.gateway.cli;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ch-sso")
public interface SsoClientService {

    @GetMapping("login/token/info")
    Result<UserInfo> tokenInfo(@RequestHeader(Constants.X_TOKEN) String token);

    @GetMapping("user/info")
    Result<UserInfo> userInfo(@RequestHeader(Constants.X_TOKEN) String token);

    @GetMapping("login/token/refresh")
    Result<UserInfo> tokenInfo(@RequestHeader(Constants.X_TOKEN) String token, @RequestHeader(Constants.X_REFRESH_TOKEN) String refreshToken);

}
