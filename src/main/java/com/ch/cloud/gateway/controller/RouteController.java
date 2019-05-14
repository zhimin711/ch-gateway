package com.ch.cloud.gateway.controller;

import com.ch.cloud.gateway.locator.ZuulRouteLocator;
import com.ch.cloud.gateway.model.Route;
import com.ch.cloud.gateway.service.IRouteService;
import com.ch.result.InvokerPage;
import com.ch.result.PageResult;
import com.ch.result.Result;
import com.ch.result.ResultUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 网关智能路由
 *
 * @author zhimin
 * @date 2019/5/12 15:12
 * @description:
 */
@Api(tags = "网关智能路由")
@RestController
@RequestMapping("gateway/route")
public class RouteController {

    @Autowired
    private IRouteService routeService;
    @Autowired
    private ZuulRouteLocator zuulRoutesLocator;

    /**
     * 获取分页路由列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页路由列表", notes = "获取分页路由列表")

    @PostMapping(value = {"{num}/{size}"})
    public PageResult<Route> page(@RequestBody Route record,
                                  @PathVariable(value = "num") int pageNum,
                                  @PathVariable(value = "size") int pageSize) {

        return ResultUtils.wrapPage(() -> {
            PageInfo<Route> page = routeService.findPage(pageNum, pageSize, record);
            return new InvokerPage.Page<>(page.getTotal(), page.getList());
        });
    }


    /**
     * 获取路由
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取路由", notes = "获取路由")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "路由ID", paramType = "path"),
    })
    @GetMapping("{id}")
    public Result<Route> getRoute(@PathVariable("id") Long id) {
        return ResultUtils.wrapFail(() -> routeService.find(id));
    }

    /**
     * 添加路由
     *
     * @return
     */
    @ApiOperation(value = "添加路由", notes = "添加路由")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "path", required = true, value = "路径表达式", paramType = "form"),
            @ApiImplicitParam(name = "serviceId", value = "服务名方转发", paramType = "form"),
            @ApiImplicitParam(name = "url", value = "地址转发", paramType = "form"),
            @ApiImplicitParam(name = "stripPrefix", allowableValues = "0,1", defaultValue = "1", value = "忽略前缀", paramType = "form"),
            @ApiImplicitParam(name = "retryable", allowableValues = "0,1", defaultValue = "0", value = "支持重试", paramType = "form"),
            @ApiImplicitParam(name = "status", allowableValues = "0,1", defaultValue = "1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "description", value = "描述", paramType = "form")
    })
    @PostMapping("save")
    public Result<Integer> add(@RequestBody Route record) {

        return ResultUtils.wrapFail(() -> {

            int c = routeService.save(record);

            if (c > 0) {
                // 刷新网关
                zuulRoutesLocator.doRefresh();
            }
            return c;
        });
    }

    /**
     * 编辑路由
     *
     * @return
     */
    @ApiOperation(value = "编辑路由", notes = "编辑路由")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "routeId", required = true, value = "路由Id", paramType = "form"),
            @ApiImplicitParam(name = "path", required = true, value = "路径表达式", paramType = "form"),
            @ApiImplicitParam(name = "serviceId", value = "服务名方转发", paramType = "form"),
            @ApiImplicitParam(name = "url", value = "地址转发", paramType = "form"),
            @ApiImplicitParam(name = "stripPrefix", allowableValues = "0,1", defaultValue = "1", value = "忽略前缀", paramType = "form"),
            @ApiImplicitParam(name = "retryable", allowableValues = "0,1", defaultValue = "0", value = "支持重试", paramType = "form"),
            @ApiImplicitParam(name = "status", allowableValues = "0,1", defaultValue = "1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "description", value = "描述", paramType = "form")
    })
    @PostMapping("save/{id}")

    public Result<Integer> edit(@PathVariable int id, @RequestBody Route record) {
        return ResultUtils.wrapFail(() -> {
            int c = routeService.updateWithNull(record);
            if (c > 0) {
                // 刷新网关
                zuulRoutesLocator.doRefresh();
            }
            return c;
        });

    }


    /**
     * 移除路由
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "移除路由", notes = "移除路由")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "id", paramType = "form"),
    })
    @PostMapping("delete")
    public Result<Integer> delete(Long id) {
        return ResultUtils.wrapFail(() -> {

            int c = routeService.delete(id);
            if (c > 0) {
                // 刷新网关
                zuulRoutesLocator.doRefresh();
            }
            return c;
        });
    }
}
