package com.ch.cloud.gateway.service;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.ch.cloud.gateway.repository.RedisRouteDefinitionRepository;
import com.ch.utils.CommonUtils;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * decs: 动态路由获取
 *
 * @author 01370603
 * @since 2019/12/20
 */
@Log4j2
@Service
public class DynamicRouteService implements ApplicationEventPublisherAware {
    
    @Autowired
    private RedisRouteDefinitionRepository routeDefinitionWriter;
    
    private ApplicationEventPublisher publisher;
    
    private final static String dataId = "ch-gateway-router.json";
    
    @Autowired
    private NacosConfigProperties nacosConfigProperties;
    
    private Set<String> routerIds;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @PostConstruct
    public void dynamicRouteByNacosListener() {
        try {
            routerIds = Sets.newHashSet();
            ConfigService configService = NacosFactory.createConfigService(
                    nacosConfigProperties.assembleConfigServiceProperties());
            
            if (CommonUtils.isEmpty(
                    stringRedisTemplate.opsForHash().values(RedisRouteDefinitionRepository.GATEWAY_ROUTES))) {
                String config = configService.getConfig(dataId, nacosConfigProperties.getGroup(), 5000);
                log.info("初始化路由信息: {}", config);
                List<RouteDefinition> gatewayRouteDefinitions = JSON.parseArray(config, RouteDefinition.class);
                for (RouteDefinition routeDefinition : gatewayRouteDefinitions) {
                    addRoute(routeDefinition);
                }
                publish();
            }
            configService.addListener(dataId, nacosConfigProperties.getGroup(), new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("更新路由信息: {}", configInfo);
                    clearRoute();
                    try {
                        List<RouteDefinition> gatewayRouteDefinitions = JSON.parseArray(configInfo,
                                RouteDefinition.class);
                        for (RouteDefinition routeDefinition : gatewayRouteDefinitions) {
                            addRoute(routeDefinition);
                        }
                        publish();
                    } catch (Exception e) {
                        log.error("configService receiveConfigInfo addRoute error!", e);
                    }
                }
                
                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
        } catch (NacosException e) {
            log.error("dynamicRoute error!", e);
        }
    }
    
    
    private void clearRoute() {
        for (String id : routerIds) {
            this.routeDefinitionWriter.delete(Mono.just(id)).subscribe();
        }
        routerIds.clear();
    }
    
    private void addRoute(RouteDefinition definition) {
        try {
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            routerIds.add(definition.getId());
        } catch (Exception e) {
            log.error("addRoute error!", e);
        }
    }
    
    public void publish() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }
    
    /**
     * 增加路由
     *
     * @param definition 路由定义
     * @return success
     */
    public String add(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        publish();
        return "success";
    }
    
    public void deleteRoute(String routeId) {
        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        publish();
    }
    
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
    
}
