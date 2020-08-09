package com.ch.cloud.gateway.controller;

import com.ch.e.PubError;
import com.ch.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public Result<?> fallback(ServerWebExchange exchange) {
        return Result.error(PubError.CONNECT, "服务暂时不可用, 请稍后重试......");
    }
}
