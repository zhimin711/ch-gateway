package com.ch.cloud.gateway.repository;

import com.alibaba.fastjson.JSON;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * decs:
 *
 * @author 01370603
 * @date 2019/12/20
 */
//@Repository
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {

    public static final String GATEWAY_ROUTES_DATA = "gateway:routes:data:";
    public static final String GATEWAY_ROUTES_KEYS = "gateway:routes:keys";

    @Resource
    private StringRedisTemplate redisTemplate;


    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> routeDefinitions = new ArrayList<>();
//        redisTemplate.opsForHash().values(GATEWAY_ROUTES)
//                .forEach(routeDefinition -> routeDefinitions.add(JSON.parseObject(routeDefinition.toString(), RouteDefinition.class)));
        List<String> keys = redisTemplate.opsForList().range(GATEWAY_ROUTES_KEYS, 0, -1);
        if (keys == null || keys.isEmpty()) return Flux.fromIterable(routeDefinitions);
        keys.forEach(r -> {
            String json = redisTemplate.opsForValue().get(GATEWAY_ROUTES_DATA + r);
            routeDefinitions.add(JSON.parseObject(json, RouteDefinition.class));
        });
        return Flux.fromIterable(routeDefinitions);

    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap((r) -> {
            redisTemplate.opsForList().leftPush(GATEWAY_ROUTES_KEYS, r.getId());
            redisTemplate.opsForValue().set(GATEWAY_ROUTES_DATA + r.getId(), JSON.toJSONString(r));
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> delete(Mono<String> routeIds) {
        return routeIds.flatMap((id) -> {
            List<String> keys = redisTemplate.opsForList().range(GATEWAY_ROUTES_KEYS, 0, -1);
            if (keys == null || keys.isEmpty()) return Mono.empty();
            if (keys.contains(id)) {
                redisTemplate.opsForList().remove(GATEWAY_ROUTES_KEYS, 1, id);
                redisTemplate.delete(GATEWAY_ROUTES_DATA + id);
                return Mono.empty();
            } else {
                return Mono.error(new NotFoundException("RouteDefinition not found: " + id));
            }
        });
    }
}
