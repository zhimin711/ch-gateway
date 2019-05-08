package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;

/**
 * 资源过滤器
 * 所有的资源请求在路由之前进行前置过滤
 * 如果请求头不包含 Authorization参数值，直接拦截不再路由
 *
 * @author 01370603
 */
public class AccessFilter extends ZuulFilter {

    private Logger logger = LoggerFactory.getLogger(AccessFilter.class);

    private final static String[] WHITELIST = {"/auth/", "/sso/", "/static/", "/assets/"};

    /**
     * 过滤器的类型 pre表示请求在路由之前被过滤
     *
     * @return 类型
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器的执行顺序
     *
     * @return 顺序 数字越大表示优先级越低，越后执行
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 过滤器是否会被执行
     * //返回一个boolean类型来判断该过滤器是否要执行，所以通过此函数可实现过滤器的开关。
     * true:总是生效，false:不生效
     *
     * @return true:总是生效，false:不生效
     */
    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String requestURI = request.getRequestURI();
        for (String s : WHITELIST) {
            if (requestURI.toLowerCase().startsWith(s)) return false;
        }
        return true;
    }

    /**
     * 过滤逻辑
     *
     * @return 过滤结果
     */
    @Override
    public Object run() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        RequestContext requestContext = RequestContext.getCurrentContext();
        requestContext.addZuulRequestHeader(Constants.TOKEN_USER, authentication.getPrincipal().toString());

        HttpServletRequest request = requestContext.getRequest();
        logger.info("send {} request to {} by {}", request.getMethod(), request.getRequestURL().toString(), authentication.getPrincipal().toString());
//
//        Object accessToken = request.getHeader("Authorization");
//        if (accessToken == null) {
//            logger.warn("Authorization token is empty");
//            requestContext.setSendZuulResponse(false);
//            requestContext.setResponseStatusCode(401);
//            requestContext.setResponseBody("Authorization token is empty");
//            return null;
//        }
//        logger.info("Authorization token is ok");
        return null;
    }
}
