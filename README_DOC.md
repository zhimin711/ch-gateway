
#### Spring Cloud Gateway高级应用

### 1. 限速路由器  
限速在高并发场景中比较常用的手段之一，可以有效的保障服务的整体稳定性，Spring Cloud Gateway 提供了基于 Redis 的限流方案。所以我们首先需要添加对应的依赖包spring-boot-starter-data-redis-reactive
```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```
配置文件中需要添加 Redis 地址和限流的相关配置
```
server:
  port: 8080
spring:
  application:
    name: spring-cloud-gateway
  redis:
    host: localhost
    password: password
    port: 6379
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
      routes:
        - id: requestratelimiter_route
          uri: http://example.org
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"
          predicates:
            - Method=GET
```
filter 名称必须是 RequestRateLimiter  
redis-rate-limiter.replenishRate：允许用户每秒处理多少个请求  
redis-rate-limiter.burstCapacity：令牌桶的容量，允许在一秒钟内完成的最大请求数  
key-resolver：使用 SpEL 按名称引用 bean  
项目中设置限流的策略，创建 Config 类。
```
package com.springcloud.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Created with IntelliJ IDEA.
 *
 * @Date: 2019/7/11
 * @Time: 23:45
 * @email: inwsy@hotmail.com
 * Description:
 */
@Configuration
public class Config {
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("user"));
    }
}
```
Config类需要加@Configuration注解。  

根据请求参数中的 user 字段来限流，也可以设置根据请求 IP 地址来限流，设置如下:
```
@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
}
```
这样网关就可以根据不同策略来对请求进行限流了。

### 2. 熔断路由器  
在之前的 Spring Cloud 系列文章中，大家对熔断应该有了一定的了解，如过不了解可以先读这篇文章：《跟我学SpringCloud | 第四篇：熔断器Hystrix》

Spring Cloud Gateway 也可以利用 Hystrix 的熔断特性，在流量过大时进行服务降级，同样我们还是首先给项目添加上依赖。
```
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```
配置示例
```
spring:
  cloud:
    gateway:
      routes:
      - id: hystrix_route
        uri: http://example.org
        filters:
        - Hystrix=myCommandName
```
配置后，gateway 将使用 myCommandName 作为名称生成 HystrixCommand 对象来进行熔断管理。如果想添加熔断后的回调内容，需要在添加一些配置。
```
spring:
  cloud:
    gateway:
      routes:
      - id: hystrix_route
        uri: lb://spring-cloud-producer
        predicates:
        - Path=/consumingserviceendpoint
        filters:
        - name: Hystrix
          args:
            name: fallbackcmd
            fallbackUri: forward:/incaseoffailureusethis
```
fallbackUri: forward:/incaseoffailureusethis配置了 fallback 时要会调的路径，当调用 Hystrix 的 fallback 被调用时，请求将转发到/incaseoffailureuset这个 URI。

### 3. 重试路由器  
RetryGatewayFilter 是 Spring Cloud Gateway 对请求重试提供的一个 GatewayFilter Factory。

配置示例
```
spring:
  cloud:
    gateway:
      routes:
      - id: retry_test
        uri: lb://spring-cloud-producer
        predicates:
        - Path=/retry
        filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY
```
Retry GatewayFilter 通过这四个参数来控制重试机制： retries, statuses, methods, 和 series。

retries：重试次数，默认值是 3 次  
statuses：HTTP 的状态返回码，取值请参考：org.springframework.http.HttpStatus
methods：指定哪些方法的请求需要进行重试逻辑，默认值是 GET 方法，取值参考：org.springframework.http.HttpMethod
series：一些列的状态码配置，取值参考：org.springframework.http.HttpStatus.Series。符合的某段状态码才会进行重试逻辑，默认值是 SERVER_ERROR，值是 5，也就是 5XX(5 开头的状态码)，共有5 个值。
以上便是项目中常用的一些网关操作，更多关于 Spring Cloud GateWay 的使用请参考官网。

[示例代码-Github](https://github.com/meteor1993/SpringCloudLearning/tree/master/chapter14)