package com.ch.cloud.gateway.filter;

import com.ch.Constants;
import com.ch.cloud.client.dto.PermissionDto;
import com.ch.cloud.client.dto.UserDto;
import com.ch.cloud.gateway.cli.SsoClientService;
import com.ch.cloud.gateway.cli.UpmsClientService;
import com.ch.result.Result;
import com.ch.utils.CommonUtils;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private SsoClientService ssoClientService;
    @Autowired
    private UpmsClientService upmsClientService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 从这里开始获取 request 中的 jwt token
        String token = request.getHeader(Constants.TOKEN_HEADER);
//        log.info("authHeader：{}", authHeader);
        // 验证token是否存在
        if (CommonUtils.isNotEmpty(token)) {
            Result<String> res = ssoClientService.tokenValidate(token);
            // 根据token 获取用户名
            if (!res.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = res.get();
                List<PermissionDto> permissions = Lists.newArrayList();
                boolean ok = false;
                for (PermissionDto dto : permissions) {
                    AntPathRequestMatcher requestMatcher = new AntPathRequestMatcher(dto.getUrl(), dto.getCode());
                    ok = requestMatcher.matches(request);
                }
                if (!ok) {
                    //
                }

            }
        }
        chain.doFilter(request, response);
    }
}
