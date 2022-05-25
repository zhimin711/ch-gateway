package com.ch.cloud.gateway.mq;

import com.ch.cloud.gateway.pojo.CacheType;
import com.ch.cloud.gateway.pojo.UserInfo;
import com.ch.cloud.upms.dto.PermissionDto;
import com.ch.pojo.KeyValue;
import com.ch.utils.CommonUtils;
import com.ch.utils.EncryptUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 描述：消息清理网关缓存
 *
 * @author Zhimin.Ma
 * @since 2022/5/25
 */
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@Component
@RocketMQMessageListener(consumerGroup = "${spring.application.name}-${spring.profiles.active}", topic = "gateway-clean")
public class NotifyCleanReceiver implements RocketMQListener<KeyValue> {

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void onMessage(KeyValue keyValue) {
        if (CommonUtils.isEmptyOr(keyValue.getKey(), keyValue.getValue())) {
            return;
        }
        switch (keyValue.getKey()) {
            case "permissions":
                cleanPermissions(keyValue);
                break;
            case "users":
                cleanUsers(keyValue);
                break;
        }
    }

    private void cleanUsers(KeyValue keyValue) {
        String md5 = EncryptUtils.md5(keyValue.getValue());
        RBucket<UserInfo> userBucket = redissonClient.getBucket(CacheType.GATEWAY_TOKEN.getKey(md5), JsonJacksonCodec.INSTANCE);
        if (userBucket.isExists()) {
            userBucket.delete();
        }
    }

    private void cleanPermissions(KeyValue keyValue) {
        RMapCache<String, List<PermissionDto>> permissionsMap = redissonClient.getMapCache(CacheType.PERMISSIONS_MAP.getKey(), JsonJacksonCodec.INSTANCE);
        if (permissionsMap.containsKey(keyValue.getValue())) {
            permissionsMap.remove(keyValue.getValue());
        } else {
            permissionsMap.clear();
        }
    }
}
