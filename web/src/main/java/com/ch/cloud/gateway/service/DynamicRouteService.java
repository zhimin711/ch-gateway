package com.ch.cloud.gateway.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.ch.cloud.gateway.repository.RedisRouteDefinitionRepository;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * decs: 动态路由获取
 *
 * @author 01370603
 * @since2019/12/20
 */
@Log4j2
@Service
public class DynamicRouteService implements ApplicationEventPublisherAware {

    @Autowired
    private RedisRouteDefinitionRepository routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    private final static String dataId = "ch-gateway-router.json";


    @Value("${nacos.config.server-addr}")
    private String serverAddr;
    @Value("${nacos.config.namespace:}")
    private String namespace;
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;

    private Set<String> routerIds;

    @PostConstruct
    public void dynamicRouteByNacosListener() {
        try {
            routerIds = Sets.newHashSet();
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
            properties.put(PropertyKeyConst.NAMESPACE, namespace);
            ConfigService configService = NacosFactory.createConfigService(properties);
            configService.getConfig(dataId, group, 5000);
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    clearRoute();
                    try {
                        List<RouteDefinition> gatewayRouteDefinitions = JSONObject.parseArray(configInfo, RouteDefinition.class);
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
            e.printStackTrace();
        }
    }

    public void publish() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * 增加路由
     *
     * @param definition
     * @return
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
