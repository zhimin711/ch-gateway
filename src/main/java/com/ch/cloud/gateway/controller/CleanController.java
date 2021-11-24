package com.ch.cloud.gateway.controller;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.result.Result;
import com.ch.utils.EncryptUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/clean")
public class CleanController {

    @Resource
    private RedissonClient redissonClient;

    @GetMapping("/user")
    public Result<Boolean> cleanUser(@RequestHeader(Constants.X_TOKEN) String token) {

        String md5 = EncryptUtils.md5(token);
        RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(md5), JsonJacksonCodec.INSTANCE);
        if (userBucket.isExists()) {
            userBucket.delete();
        }
        return Result.success();
    }
}
