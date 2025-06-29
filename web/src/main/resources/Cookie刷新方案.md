# Cookie自动刷新方案

## 概述

本方案实现了Cookie token的自动刷新机制，当Cookie即将过期时（默认5分钟内），系统会自动刷新Cookie，延长其有效期，确保用户无需重新登录。

## 架构设计

### 1. 核心组件

- **CookieRefreshFilter**: Cookie刷新过滤器，拦截所有请求检查Cookie状态
- **CookieRefreshService**: Cookie刷新服务，封装刷新业务逻辑
- **CookieConfig**: Cookie配置类，管理所有Cookie相关配置

### 2. 工作流程

```
请求到达 → CookieRefreshFilter → 检查Cookie状态 → 需要刷新？ → 刷新Cookie → 继续处理
```

## 配置说明

### 配置文件位置
- 本地环境: `config/application-local.yml`
- 测试环境: `config/application-test.yml`
- 生产环境: `config/application-prod.yml`

### 配置参数

```yaml
gateway:
  cookie:
    token-name: TOKEN          # Cookie名称
    max-age: 1800             # Cookie最大存活时间（秒）- 30分钟
    refresh-threshold: 300    # 刷新阈值（秒）- 5分钟内过期时刷新
    path: /                   # Cookie路径
    http-only: true           # 是否仅HTTP访问
    secure: false             # 是否仅HTTPS访问（生产环境建议true）
    auto-refresh: true        # 是否启用自动刷新
    enable-log: true          # 是否启用刷新日志
```

## 实现细节

### 1. Cookie刷新检查

```java
public boolean needRefreshCookie(String token) {
    // 1. 检查是否启用自动刷新
    if (!cookieConfig.isAutoRefresh()) {
        return false;
    }
    
    // 2. 从Redis获取用户信息
    String md5 = EncryptUtils.md5(token);
    RBucket<Object> userBucket = redissonClient.getBucket(
            CacheType.GATEWAY_TOKEN.getKey(md5), JsonJacksonCodec.INSTANCE);
    
    // 3. 检查用户信息是否存在
    if (!userBucket.isExists()) {
        return false;
    }
    
    // 4. 获取过期时间并计算剩余时间
    Object user = userBucket.get();
    long timeToExpire = calculateTimeToExpire(user);
    
    // 5. 判断是否需要刷新
    return timeToExpire <= cookieConfig.getRefreshThreshold() * 1000;
}
```

### 2. Cookie刷新执行

```java
public void refreshCookie(ServerHttpResponse response, String token) {
    // 1. 创建新的Cookie
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
            cookieConfig.getTokenName(), token);
    
    // 2. 设置Cookie属性
    builder.maxAge(cookieConfig.getMaxAge());
    builder.path(cookieConfig.getPath());
    builder.httpOnly(cookieConfig.isHttpOnly());
    builder.secure(cookieConfig.isSecure());
    
    // 3. 添加到响应头
    ResponseCookie newCookie = builder.build();
    response.addCookie(newCookie);
}
```

## 使用场景

### 1. 正常使用场景
- 用户登录后，Cookie有效期为30分钟
- 用户在25分钟后访问系统，Cookie自动刷新
- 用户无需感知刷新过程，继续正常使用

### 2. 异常处理场景
- Cookie已过期：返回401错误，要求重新登录
- Token无效：清除Cookie，返回401错误
- 网络异常：记录错误日志，不影响正常请求

## 安全考虑

### 1. Cookie安全属性
- `httpOnly: true` - 防止XSS攻击
- `secure: true` - 生产环境强制HTTPS
- `path: /` - 限制Cookie作用域

### 2. 刷新策略
- 仅在即将过期时刷新，避免频繁刷新
- 刷新阈值可配置，默认5分钟
- 支持禁用自动刷新功能

### 3. 错误处理
- 刷新失败不影响正常请求处理
- 详细的错误日志记录
- 优雅的降级处理

## 监控和日志

### 1. 日志级别
- DEBUG: 详细的刷新过程日志
- INFO: 重要的刷新事件
- ERROR: 刷新失败错误

### 2. 监控指标
- Cookie刷新次数
- 刷新成功率
- 刷新耗时统计

## 部署说明

### 1. 环境配置
```bash
# 本地开发环境
cp config/application-local.yml config/application.yml

# 生产环境
cp config/application-prod.yml config/application.yml
```

### 2. 启动参数
```bash
# 启用Cookie刷新功能
java -jar ch-gateway.jar --gateway.cookie.auto-refresh=true

# 禁用Cookie刷新功能
java -jar ch-gateway.jar --gateway.cookie.auto-refresh=false
```

## 故障排查

### 1. 常见问题

**Q: Cookie没有自动刷新？**
A: 检查配置项 `gateway.cookie.auto-refresh` 是否为true

**Q: 刷新频率过高？**
A: 调整 `gateway.cookie.refresh-threshold` 参数

**Q: 生产环境Cookie不安全？**
A: 设置 `gateway.cookie.secure=true`

### 2. 日志查看
```bash
# 查看Cookie刷新日志
tail -f logs/ch-gateway/gateway.log | grep "Cookie"

# 查看错误日志
tail -f logs/ch-gateway/gateway.log | grep "ERROR"
```

## 扩展功能

### 1. 多租户支持
- 支持不同租户的Cookie配置
- 租户级别的刷新策略

### 2. 智能刷新
- 基于用户行为模式的智能刷新
- 预测性刷新机制

### 3. 分布式刷新
- 支持集群环境下的Cookie同步
- 跨服务的Cookie一致性

## 总结

本Cookie自动刷新方案具有以下特点：

1. **自动化**: 无需用户干预，自动处理Cookie刷新
2. **可配置**: 支持灵活的配置参数调整
3. **安全性**: 考虑多种安全因素，保护用户数据
4. **可监控**: 提供完整的日志和监控支持
5. **高可用**: 具备完善的错误处理和降级机制

通过该方案，可以有效提升用户体验，减少因Cookie过期导致的重新登录问题。 