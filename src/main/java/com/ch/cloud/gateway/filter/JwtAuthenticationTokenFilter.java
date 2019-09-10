package com.ch.cloud.gateway.filter;

import com.ch.Constants;
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
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

                Result<UserDto> res1 = upmsClientService.findUserByUsername(username);
                // 通过用户名 获取用户的信息
                UserDetails userDetails = new User(res1.get().getUsername(),res1.get().getPassword(), Lists.newArrayList());
                // 然后把构造UsernamePasswordAuthenticationToken对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // 最后绑定到当前request中，在后面的请求中就可以获取用户信息
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
