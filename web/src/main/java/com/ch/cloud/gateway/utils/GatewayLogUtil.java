package com.ch.cloud.gateway.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.ch.Constants;
import com.ch.cloud.gateway.decorator.RecorderServerHttpResponseDecorator;
import com.ch.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Log4j2
public class GatewayLogUtil {
    
    private GatewayLogUtil() {
    }
    
    private static final String REQUEST_RECORDER_LOG_BUFFER = "RequestRecorderGlobalFilter.request_recorder_log_buffer";
    
    private static final String BODY_BUFFER = "RequestRecorderGlobalFilter.request_body_buffer";
    
    private static final String REQUEST_PROCESS_SEPARATOR = "\n[REQUEST_PROCESS_SEPARATOR]\n";
    
    private static final String HEADER_COOKIE_KEY = "Cookie";
    
    private static boolean hasBody(HttpMethod method) {
        //只记录这3种谓词的body
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }
    
    public static boolean shouldRecordBody(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        String type = contentType.getType();
        String subType = contentType.getSubtype();
        
        if ("application".equals(type)) {
            return "json".equals(subType) || "x-www-form-urlencoded".equals(subType) || "xml".equals(subType)
                    || "atom+xml".equals(subType) || "rss+xml".equals(subType);
        } else {
            return "text".equals(type);
        }
        
        //form没有记录
    }
    
    private static Mono<Void> doRecordBody(StringBuffer buffer, Flux<DataBuffer> body, Charset charset,
            boolean isResponse, JSONObject json) {
        return DataBufferFixUtil.join(body).doOnNext(wrapper -> {
            
            String data = new String(wrapper.getData(), charset);
            JSONValidator from = JSONValidator.from(data);
            if (from.validate()) {
                JSONValidator.Type type = from.getType();
                
                if (type == JSONValidator.Type.Array) {
                    JSONArray array = JSONArray.parseArray(data);
                    if (isResponse) {
                        json.put("body", array.subList(0, Math.min(array.size(), 1)));
                    } else {
                        json.put("body", array);
                    }
                } else if (type == JSONValidator.Type.Object) {
                    JSONObject obj = JSONObject.parseObject(data);
                    if (isResponse && obj.containsKey("rows")) {
                        JSONArray rowsArr = obj.getJSONArray("rows");
                        obj.put("rows", rowsArr.subList(0, Math.min(rowsArr.size(), 1)));
                    }
                    json.put("body", obj);
                } else {
                    json.put("body", data);
                }
            } else {
                json.put("body", data);
            }
            if (isResponse) {
                buffer.append("{\"response\":");
            } else {
                buffer.append("{\"request\":");
            }
            buffer.append(json.toJSONString());
            buffer.append("}");
            wrapper.clear();
        }).then();
    }
    
    private static String subData(String data) {
        try {
            JSONObject obj = JSONObject.parseObject(data);
            if (obj.containsKey("rows")) {
                JSONArray rowsArr = obj.getJSONArray("rows");
                obj.put("rows", rowsArr.subList(0, Math.min(rowsArr.size(), 10)));
            }
            return obj.toJSONString();
        } catch (Exception ignored) {
        }
        return data;
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
        return recorderRequest(request, logBuffer);
    }
    
    public static Mono<Void> recorderRouteRequest(ServerWebExchange exchange) {
        URI uri = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        StringBuffer logBuffer = exchange.getAttribute(REQUEST_RECORDER_LOG_BUFFER);
        if (logBuffer == null) {
            logBuffer = new StringBuffer();
        } else {
            logBuffer.append(REQUEST_PROCESS_SEPARATOR);
        }
        
        logBuffer.append("{\"").append("proxy").append("\":{");
        appendKeyValueEnd(logBuffer, "url", uri.toString());
        logBuffer.append("}}");
        return Mono.empty();
        
        //        return recorderRequest(exchange.getRequest(), uri, logBuffer);
    }
    
    private static void appendKeyValue(StringBuffer logBuffer, String key, String value) {
        logBuffer.append("\"").append(key).append("\":\"").append(value).append("\"").append(",");
    }
    
    private static void appendKeyValueEnd(StringBuffer logBuffer, String key, String value) {
        logBuffer.append("\"").append(key).append("\":\"").append(value).append("\"");
    }
    
    private static Mono<Void> recorderRequest(ServerHttpRequest request, StringBuffer logBuffer) {
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("url", request.getURI().toString());
        
        HttpMethod method = request.getMethod();
        if (method != null) {
            requestJSON.put("method", method.name());
        }
        HttpHeaders headers = request.getHeaders();
        JSONObject jsonHeader = convertHeadersJSON(headers);
        requestJSON.put("headers", jsonHeader);
        JSONObject jsonCookie = convertCookiesJSON(request.getCookies());
        requestJSON.put("cookies", jsonCookie);
        
        Charset bodyCharset = null;
        if (hasBody(method)) {
            long length = headers.getContentLength();
            requestJSON.put("contentLength", length);
            if (length > 0) {
                MediaType contentType = headers.getContentType();
                requestJSON.put("contentType", contentType);
                if (shouldRecordBody(contentType)) {
                    bodyCharset = getMediaTypeCharset(contentType);
                }
            }
        }
        if (bodyCharset != null) {
            return doRecordBody(logBuffer, request.getBody(), bodyCharset, false, requestJSON);
        } else {
            logBuffer.append("{\"request\":").append(requestJSON.toJSONString()).append("}");
            return Mono.empty();
        }
    }
    
    private static JSONObject convertCookiesJSON(MultiValueMap<String, ? extends HttpCookie> cookies) {
        JSONObject cookiesJSON = new JSONObject();
        if (cookies.isEmpty()) {
            return cookiesJSON;
        }
        
        cookies.forEach((name, values) -> {
            List<String> collect = values.stream().map(HttpCookie::getValue).collect(Collectors.toList());
            if (collect.size() == 1) {
                cookiesJSON.put(name, collect.get(0));
            } else {
                cookiesJSON.put(name, collect);
            }
        });
        return cookiesJSON;
    }
    
    private static JSONObject convertHeadersJSON(HttpHeaders headers) {
        JSONObject headersJSON = new JSONObject();
        if (headers.isEmpty()) {
            return headersJSON;
        }
        headers.forEach((name, values) -> {
            if (HEADER_COOKIE_KEY.equalsIgnoreCase(name)) {
                return;
            }
            if (values.size() == 1) {
                headersJSON.put(name, values.get(0));
            } else {
                headersJSON.put(name, values);
            }
        });
        return headersJSON;
    }
    
    private static void recorderHeader(StringBuffer logBuffer, HttpHeaders headers) {
        logBuffer.append("\"headers\":{");
        AtomicInteger i = new AtomicInteger();
        headers.forEach((name, values) -> {
            if (values.size() == 1) {
                appendKeyValueEnd(logBuffer, name, values.get(0).replaceAll("\"", "'"));
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
        
        HttpStatus code = response.getStatusCode();
        if (code == null) {
            logBuffer.append("{\"").append("response").append("\":");
            logBuffer.append("\"返回异常\"}");
            return Mono.empty();
        }
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("status", code.value());
        
        HttpHeaders headers = response.getHeaders();
        JSONObject jsonHeader = convertHeadersJSON(headers);
        responseJSON.put("headers", jsonHeader);
        JSONObject jsonCookie = convertCookiesJSON(response.getCookies());
        responseJSON.put("cookies", jsonCookie);
        
        Charset bodyCharset = null;
        if (shouldRecordBody(headers.getContentType())) {
            bodyCharset = getMediaTypeCharset(headers.getContentType());
        }
        //        boolean isDecorator = exchange.getResponse() instanceof RecorderServerHttpResponseDecorator;
        
        if (bodyCharset != null) {
            return doRecordBody(logBuffer, ((RecorderServerHttpResponseDecorator) response).copy(), bodyCharset, true,
                    responseJSON);
        } else {
            logBuffer.append("{\"response\":").append(responseJSON.toJSONString()).append("}");
            return Mono.empty();
        }
    }
    
    public static String getLogData(ServerWebExchange exchange) {
        return getLogData(exchange, 0, 0);
    }
    
    public static String getLogData(ServerWebExchange exchange, long startTimeMillis, long endTimeMillis) {
        StringBuffer logBuffer = exchange.getAttribute(REQUEST_RECORDER_LOG_BUFFER);
        if (logBuffer == null) {
            return null;
        }
        logBuffer.append(REQUEST_PROCESS_SEPARATOR);
        logBuffer.append("{\"").append("record").append("\":{");
        ServerHttpRequest request = exchange.getRequest();
        appendKeyValue(logBuffer, "url", request.getPath().value());
        appendKeyValue(logBuffer, "method", request.getMethodValue());
        String username = request.getHeaders().getFirst(Constants.X_TOKEN_USER);
        if (CommonUtils.isNotEmpty(username)) {
            appendKeyValue(logBuffer, "username", username);
        }
        appendKeyValue(logBuffer, "startTimestamp", String.valueOf(startTimeMillis));
        appendKeyValueEnd(logBuffer, "endTimestamp", String.valueOf(endTimeMillis));
        logBuffer.append("}}");
        return logBuffer.toString();
    }
}
