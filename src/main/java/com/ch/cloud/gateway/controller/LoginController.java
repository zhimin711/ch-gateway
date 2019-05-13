package com.ch.cloud.gateway.controller;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.ch.result.Result;
import com.ch.result.ResultUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * desc:
 *
 * @author zhimin
 * @date 2018/12/21 10:40 PM
 */
@RestController
public class LoginController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

//    @NacosValue(value = "${security.oauth2.client.client-id}", autoRefreshed = true)
    private String clientId;
//    @NacosValue(value = "${security.oauth2.client.client-id}", autoRefreshed = true)
    private String clientSecret;

    /**
     * 获取用户访问令牌
     * 基于oauth2密码模式登录
     *
     * @param username
     * @param password
     * @return access_token
     */
    @ApiOperation(value = "获取用户访问令牌", notes = "基于oauth2密码模式登录,无需签名,返回access_token")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "登录名", paramType = "form"),
            @ApiImplicitParam(name = "password", required = true, value = "登录密码", paramType = "form")
    })
    @PostMapping("login/token/access")
    public Result<Long> getLoginToken(@RequestParam String username, @RequestParam String password, @RequestHeader HttpHeaders headers) {
        // 使用oauth2密码模式登录.
        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("username", username);
        postParameters.add("password", password);
        postParameters.add("client_id", clientId);
        postParameters.add("client_secret", clientSecret);
        postParameters.add("grant_type", "password");
        // 使用客户端的请求头,发起请求
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 强制移除 原来的请求头,防止token失效
        headers.remove(HttpHeaders.AUTHORIZATION);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);
        return ResultUtils.wrap(() -> 1L);
    }
}
