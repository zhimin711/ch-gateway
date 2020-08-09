package com.ch.cloud.gateway;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class LoginTests {

    @Test
    public void login() {
        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("username", "admin");
        postParameters.add("password", "123456");
        postParameters.add("client_id", "");
        postParameters.add("client_secret", "");
        postParameters.add("grant_type", "password");
        HttpHeaders headers = new HttpHeaders();
        // 使用客户端的请求头,发起请求
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 强制移除 原来的请求头,防止token失效
        headers.remove(HttpHeaders.AUTHORIZATION);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);
    }

    @Test
    public void testUrl(){

//        AntPathRequestMatcher requestMatcher = new AntPathRequestMatcher("/user/[0-9]/[0-9]");
        AntPathMatcher pathMatcher = new AntPathMatcher("/");
        System.out.println(pathMatcher.match("/user/{page:[0-9]+}/{size:[0-9]+}","/user/12/10"));
        System.out.println(pathMatcher.match("/user/{id:[0-9]+}","/user/a"));
        System.out.println(pathMatcher.match("/upms/department/{id:[0-9]+}/positions/{name}","/upms/department/1/positions/b"));
    }
}
