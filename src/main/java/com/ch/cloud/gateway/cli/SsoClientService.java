package com.ch.cloud.gateway.cli;

import com.ch.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ch-sso")
public interface SsoClientService {

    @GetMapping("login/token/validate")
    Result<String> tokenValidate(@RequestParam("token") String token);
}
