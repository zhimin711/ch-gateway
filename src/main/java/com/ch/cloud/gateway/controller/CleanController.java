package com.ch.cloud.gateway.controller;

import com.ch.Constants;
import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.result.Result;
import com.ch.utils.EncryptUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    @GetMapping("/role/{role:[0-9]+}/permissions")
    Result<Boolean> cleanRolePermissions(@PathVariable("role") Long roleId) {
        RMapCache<String, List<PermissionDto>> permissionsMap = redissonClient.getMapCache(CacheType.PERMISSIONS_MAP.getKey(), JsonJacksonCodec.INSTANCE);
        permissionsMap.remove(roleId.toString());
        return Result.success();
    }

    @GetMapping("/permissions")
    Result<Boolean> cleanPermissions() {
        RMapCache<String, List<PermissionDto>> permissionsMap = redissonClient.getMapCache(CacheType.PERMISSIONS_MAP.getKey(), JsonJacksonCodec.INSTANCE);
        permissionsMap.clear();
        return Result.success();
    }
}
