package com.ch.cloud.gateway.service.impl;

import com.ch.cloud.gateway.mapper.RouteMapper;
import com.ch.cloud.gateway.model.Route;
import com.ch.cloud.gateway.service.IRouteService;
import com.ch.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

import javax.annotation.Resource;

@Service
public class RouteServiceImpl extends BaseService<Long, Route> implements IRouteService {

    @Resource
    private RouteMapper routeMapper;

    @Override
    protected Mapper<Route> getMapper() {
        return routeMapper;
    }
}
