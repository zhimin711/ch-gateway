package com.ch.cloud.gateway.cli;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ch-sso")
public interface SsoClientService {

    @GetMapping("login/token/info")
    Result<UserInfo> tokenInfo(@RequestHeader(Constants.TOKEN_HEADER2) String token);

}
