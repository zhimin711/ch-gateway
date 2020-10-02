package com.ch.cloud.gateway.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.ch.cloud.gateway.pojo.GatewayRoute;
import com.ch.cloud.gateway.repository.RedisRouteDefinitionRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * decs:
 *
 * @author 01370603
 * @date 2019/12/20
 */
@Log4j2
@Service
public class DynamicRouteService implements ApplicationEventPublisherAware {

    //    @Autowired
//    private RouteDefinitionWriter routeDefinitionWriter;
    @Autowired
    private RedisRouteDefinitionRepository routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    private String dataId = "ch-gateway-router.json";

    private String group = "DEFAULT_GROUP";

    @Value("${nacos.config.server-addr}")
    private String serverAddr;
    @Value("${nacos.config.namespace}")
    private String namespace;

    private Set<String> routerIds;

    @PostConstruct
    public void dynamicRouteByNacosListener() {
        try {
            routerIds = Sets.newHashSet();
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            properties.setProperty("namespace", namespace);
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

    public String loadRouteConfig() {
        //从数据库拿到路由配置
        List<GatewayRoute> gatewayRouteList = Lists.newArrayList();//gatewayRouteMapper.queryAllRoutes();

        log.info("网关配置信息：=====>" + JSON.toJSONString(gatewayRouteList));
        gatewayRouteList.forEach(gatewayRoute -> {
            RouteDefinition definition = new RouteDefinition();
            Map<String, String> predicateParams = new HashMap<>(8);
            PredicateDefinition predicate = new PredicateDefinition();
            FilterDefinition filterDefinition = new FilterDefinition();
            Map<String, String> filterParams = new HashMap<>(8);

            URI uri = null;
            if (gatewayRoute.getUri().startsWith("http")) {
                //http地址
                uri = UriComponentsBuilder.fromHttpUrl(gatewayRoute.getUri()).build().toUri();
            } else {
                //注册中心
                uri = UriComponentsBuilder.fromUriString("lb://" + gatewayRoute.getUri()).build().toUri();
            }

            definition.setId(gatewayRoute.getId().toString());
            // 名称是固定的，spring gateway会根据名称找对应的PredicateFactory
            predicate.setName("Path");
            predicateParams.put("pattern", gatewayRoute.getPredicates());
            predicate.setArgs(predicateParams);

            // 名称是固定的, 路径去前缀
            filterDefinition.setName("StripPrefix");
            filterParams.put("_genkey_0", gatewayRoute.getFilters());
            filterDefinition.setArgs(filterParams);

            definition.setPredicates(Collections.singletonList(predicate));
            definition.setFilters(Collections.singletonList(filterDefinition));
            definition.setUri(uri);
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        });
        publish();
        return "success";
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
