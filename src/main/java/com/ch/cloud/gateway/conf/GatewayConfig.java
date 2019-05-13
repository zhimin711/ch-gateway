package com.ch.cloud.gateway.conf;


import com.ch.cloud.gateway.locator.ZuulRouteLocator;
import com.ch.cloud.gateway.service.IRouteService;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {


    private ZuulRouteLocator zuulRoutesLocator;


    /**
     * 初始化路由加载器
     *
     * @return
     */
    @Bean
    public ZuulRouteLocator zuulRouteLocator(ZuulProperties zuulProperties, ServerProperties serverProperties, IRouteService routeService) {
        zuulRoutesLocator = new ZuulRouteLocator(serverProperties.getServlet().getContextPath(), zuulProperties, routeService);
//        log.info("ZuulRoutesLocator:{}", zuulRoutesLocator);
        return zuulRoutesLocator;
    }
}
