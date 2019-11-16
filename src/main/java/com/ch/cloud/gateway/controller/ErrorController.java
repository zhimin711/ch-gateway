package com.ch.cloud.gateway.controller;

import com.ch.e.PubError;
import com.ch.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {

    @GetMapping("/fallback")
    public Result<?> fallback() {
        return Result.error(PubError.CONNECT, "服务暂时不可用, 稍后重试......");
    }
}
