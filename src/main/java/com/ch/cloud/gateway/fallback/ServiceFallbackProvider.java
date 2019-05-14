package com.ch.cloud.gateway.fallback;

import com.ch.e.PubError;
import com.ch.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * desc:
 *
 * @author zhimin
 * @date 2019/4/15 7:41 PM
 */
@Component
public class ServiceFallbackProvider implements FallbackProvider {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        logger.error("{} route err!", route, cause);
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() {
                return getStatusCode().value();
            }

            @Override
            public String getStatusText() {
                return getStatusCode().getReasonPhrase();
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                //响应体
                ObjectMapper objectMapper = new ObjectMapper();
                String content = objectMapper.writeValueAsString(Result.error(PubError.CONNECT, route + "微服务不可用，请稍后再试..."));
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
                return httpHeaders;
            }
        };
    }


    @Override
    public String getRoute() {
        //表明是为哪个微服务提供回退，"*"全部
        return "*";
    }
}