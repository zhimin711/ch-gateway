# ch-gateway（朝华网关微服务）

#### 介绍
网关是提供前端请求滤过与路由的基础服务，将请求鉴权后分发到对应的微服务。

* 使用Spring Boot + Spring gateway框架
* 采用Alibaba Nacos 为注册与配置中心
* 使用Redis缓存用户基本信息与角色权限
* 使用RocketMQ做消息总线存储日志
* 使用Alibaba Sentinel 做流量哨兵,提供限流与熔断（配置默认关闭）
* 结合Nacos配置实现动态路由前缓存Redis

#### 软件架构
请参见Wiki文档 [传送门](https://gitee.com/ch-cloud/wiki)


#### 安装教程

1. 修改配置文件（基于Wiki基础服务）  
（1） resources/config/application-local.yml  
>修改  
redis.host与redis.port
rocketmq.name-server

```yaml
server:
  port: 7001

jasypt:
  encryptor:
    password: abc123
#    algorithm: PBEWithMD5AndDES
spring:
  redis:
    ##
    jedis:
      pool:
        ### 连接池最大连接数（使用负值表示没有限制）
        max-active: 9
        ### 连接池最大阻塞等待时间（使用负值表示没有限制）
        max-wait: -1
        ### 连接池中的最大空闲连接
        max-idle: 9
        ### 连接池中的最小空闲连接
        min-idle: 0
    ### 连接超时时间（毫秒）
    timeout: 60000
    ### Redis数据库索引(默认为0)
    host: 192.168.199.194
    port: 6379
#    password: *iwe
    database: 0
#    sentinel:
#      master: SHIVA_TRTMS_GROUND_REDIS_SESSION_C01
#      nodes:
#        - session1.ch.com:8001
#        - session2.ch.com:8001
#        - session3.ch.com:8001
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
  #API网关配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://www.zhaohuajike.cn:3306/dev_ch_gateway
    username: admin
    password: ENC(hfO3JFDCY2HB6x+j1obZOg==)
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 5
      maximum-pool-size: 15
      auto-commit: true
      idle-timeout: 30000
      pool-name: DatebookHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  #表明gateway开启服务注册和发现的功能，并且spring cloud gateway自动根据服务发现为每一个服务创建了一个router，这个router将以服务名开头的请求路径转发到对应的服务。
          lowerCaseServiceId: true   #是将请求路径上的服务名配置为小写（因为服务注册的时候，向注册中心注册时将服务名转成大写的了），比如以/service-hi/*的请求路径被路由转发到服务名为service-hi的服务上。
          filters:
            - StripPrefix=1
      routes:
        - id: sso
          uri: lb://ch-sso
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=1
        - id: upms
          uri:  lb://ch-upms
          predicates:
            - Path=/upms/**
          filters:
            - StripPrefix=1
        - id: kafka
          uri:  lb://ch-kafka
          predicates:
            - Path=/kafka/**
          filters:
            - StripPrefix=1
      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY
            series: SERVER_ERROR
        - name: Hystrix
          args:
            name: fallbackcmd
            fallbackUri: forward:/fallback
#      loadBalanced: true
##############end#####################
####超时配置####
ribbon:
  ReadTimeout: 10000
  ConnectTimeout: 10000
  MaxAutoRetries: 1
  MaxAutoRetriesNextServer: 2
  http:
    client:
      enabled: true
hystrix:
  command:
    default:
      execution:
        timeout:
          enabled: true
        isolation:
          thread:
            timeoutInMilliseconds: 600000
###超时配置###

logging:
  config: classpath:config/logback-test.xml
  path: logs/ch-gateway
  level:
    com.ch: debug
    org.springframework.cloud: info
    com.alibaba.nacos: warn
rocketmq:
  name-server: 192.168.199.194:9876 # 自己的RocketMQ服务地址
  producer:
    send-message-timeout: 300000
    group: ch-gateway
```
（2） resources/bootstrap.yml  
修改namespace与server-addr
```yaml
nacos:
  config:
    namespace: local
    server-addr: 192.168.199.194:8848
spring:
  application:
    name: ch-gateway
  cloud:
    nacos:
      discovery:
        server-addr: ${nacos.config.server-addr}
        namespace: ${nacos.config.namespace:}
      config:
        server-addr: ${nacos.config.server-addr}
        namespace: ${nacos.config.namespace:}
        shared-dataids: ch-gateway.yml
#    sentinel:
#      transport:
#        dashboard: 192.168.199.194:8800
#        port: 8800
#      # 服务启动直接建立心跳连接
#      eager: true
#      datasource:
#        ds1:
#          nacos:
#            server-addr: ${nacos.config.server-addr}
#            dataId: ${spring.application.name}-flow-rules.json
#            data-type: json
#            rule-type: flow
```
2. 上传配置文件（application-local.yml,注:文件名要修改为"应用名称".yml（spring.application.name））到Nacos  
上传动态路由文件ch-gateway-router.json到Nacos
3. 启动服务
~~~
#gradle工具命令启动：
gradle bootJar
#docker部署参考other目录deploy.md
~~~



#### 参与贡献

1. Fork 本仓库
