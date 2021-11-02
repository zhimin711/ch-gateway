package com.ch.cloud.gateway.utils;

import com.alibaba.fastjson.JSONObject;
import com.ch.Constants;
import com.ch.cloud.gateway.decorator.RecorderServerHttpResponseDecorator;
import com.ch.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Log4j2
public class GatewayLogUtil {
    private final static String REQUEST_RECORDER_LOG_BUFFER = "RequestRecorderGlobalFilter.request_recorder_log_buffer";
    private final static String REQUEST_PROCESS_SEPARATOR = "\n[REQUEST_PROCESS_SEPARATOR]\n";

    private static boolean hasBody(HttpMethod method) {
        //只记录这3种谓词的body
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    public static boolean shouldRecordBody(MediaType contentType) {
        if (contentType == null) return false;
        String type = contentType.getType();
        String subType = contentType.getSubtype();

        if ("application".equals(type)) {
            return "json".equals(subType) || "x-www-form-urlencoded".equals(subType) || "xml".equals(subType) || "atom+xml".equals(subType) || "rss+xml".equals(subType);
        } else return "text".equals(type);

        //form没有记录
    }

    private static Mono<Void> doRecordBody(StringBuffer logBuffer, Flux<DataBuffer> body, Charset charset) {
        return DataBufferFixUtil.join(body)
                .doOnNext(wrapper -> {
                    logBuffer.append("\"data\":");
                    logBuffer.append(new String(wrapper.getData(), charset));
                    logBuffer.append("}}");
                    wrapper.clear();
                }).then();
    }

    private static Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
        if (mediaType != null && mediaType.getCharset() != null) {
            return mediaType.getCharset();
        } else {
            return StandardCharsets.UTF_8;
        }
    }

    public static Mono<Void> recorderOriginalRequest(ServerWebExchange exchange) {
        StringBuffer logBuffer = new StringBuffer();
        exchange.getAttributes().put(REQUEST_RECORDER_LOG_BUFFER, logBuffer);

        ServerHttpRequest request = exchange.getRequest();
        logBuffer.append("{\"").append("request").append("\":");
        return recorderRequest(request, request.getURI(), logBuffer);
    }

    public static Mono<Void> recorderRouteRequest(ServerWebExchange exchange) {
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        StringBuffer logBuffer = exchange.getAttribute(REQUEST_RECORDER_LOG_BUFFER);
        if (logBuffer == null) {
            logBuffer = new StringBuffer();
        } else {
            logBuffer.append(REQUEST_PROCESS_SEPARATOR);
        }
        logBuffer.append("{\"").append("proxy").append("\":");
        return recorderRequest(exchange.getRequest(), requestUrl, logBuffer);
    }

    private static void appendKeyValue(StringBuffer logBuffer, String key, String value) {
        logBuffer.append("\"").append(key).append("\":\"").append(value).append("\"").append(",");
    }

    private static void appendKeyValueEnd(StringBuffer logBuffer, String key, String value) {
        logBuffer.append("\"").append(key).append("\":\"").append(value).append("\"");
    }

    private static Mono<Void> recorderRequest(ServerHttpRequest request, URI uri, StringBuffer logBuffer) {
        if (uri == null) {
            uri = request.getURI();
        }
        logBuffer.append("{");
        appendKeyValue(logBuffer, "url", uri.toString());

        HttpMethod method = request.getMethod();
        if (method != null) {
            appendKeyValue(logBuffer, "method", method.name());
        }
        HttpHeaders headers = request.getHeaders();
        recorderHeader(logBuffer, headers);

        Charset bodyCharset = null;
        if (hasBody(method)) {
            long length = headers.getContentLength();
            if (length > 0) {
                MediaType contentType = headers.getContentType();
                logBuffer.append(",");
                appendKeyValue(logBuffer, "contentType", contentType != null ? contentType.toString() : "");
                if (shouldRecordBody(contentType))
                    bodyCharset = getMediaTypeCharset(contentType);
            }
        }

        if (bodyCharset != null) {
            return doRecordBody(logBuffer, request.getBody(), bodyCharset);
        } else {
            logBuffer.append("}}");
            return Mono.empty();
        }

    }

    private static void recorderHeader(StringBuffer logBuffer, HttpHeaders headers) {
        logBuffer.append("\"headers\":{");
        AtomicInteger i = new AtomicInteger();
        headers.forEach((name, values) -> {
            if (values.size() == 1) {
                appendKeyValueEnd(logBuffer, name, values.get(0).replaceAll("\"","'"));
            } else {
                logBuffer.append("\"").append(name).append("\":").append(JSONObject.toJSONString(values));
            }
            int c = i.getAndIncrement();
            if (c < headers.size() - 1) {
                logBuffer.append(",");
            }
        });
        logBuffer.append("}");
    }

    public static Mono<Void> recorderResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        StringBuffer logBuffer = exchange.getAttribute(REQUEST_RECORDER_LOG_BUFFER);
        Objects.requireNonNull(logBuffer);
        logBuffer.append(REQUEST_PROCESS_SEPARATOR);

        logBuffer.append("{\"").append("response").append("\":");

        HttpStatus code = response.getStatusCode();
        if (code == null) {
            logBuffer.append("{\"返回异常\"}");
            return Mono.empty();
        }
        logBuffer.append("{");
        appendKeyValue(logBuffer, "status", code.value() + "");

        HttpHeaders headers = response.getHeaders();
        recorderHeader(logBuffer, headers);

        Charset bodyCharset = null;
        if (shouldRecordBody(headers.getContentType())) {
            bodyCharset = getMediaTypeCharset(headers.getContentType());
        }
//        boolean isDecorator = exchange.getResponse() instanceof RecorderServerHttpResponseDecorator;
        if (bodyCharset != null) {
            logBuffer.append(",");
            return doRecordBody(logBuffer, ((RecorderServerHttpResponseDecorator) response).copy(), bodyCharset);
        } else {
            logBuffer.append("}}");
            return Mono.empty();
        }
    }

    public static String getLogData(ServerWebExchange exchange) {
        return getLogData(exchange, 0, 0);
    }

    public static String getLogData(ServerWebExchange exchange, long startTimeMillis, long endTimeMillis) {
        StringBuffer logBuffer = exchange.getAttribute(REQUEST_RECORDER_LOG_BUFFER);
        if (logBuffer == null) return null;
        logBuffer.append(REQUEST_PROCESS_SEPARATOR);
        logBuffer.append("{\"").append("record").append("\":{");
        ServerHttpRequest request = exchange.getRequest();
        appendKeyValue(logBuffer, "url", request.getPath().value());
        appendKeyValue(logBuffer, "method", request.getMethodValue());
        List<String> userList = request.getHeaders().getOrEmpty(Constants.X_TOKEN_USER);
        if (CommonUtils.isNotEmpty(userList)) {
            appendKeyValue(logBuffer, "username", userList.get(0));
        }
        appendKeyValue(logBuffer, "startTimestamp", startTimeMillis + "");
        appendKeyValueEnd(logBuffer, "endTimestamp", endTimeMillis + "");
        logBuffer.append("}}");
        return logBuffer.toString();
    }
}
