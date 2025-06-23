# 权限过滤器架构说明

## 概述

本网关系统采用分层权限过滤器架构，将不同类型的权限验证分离到独立的过滤器中，提高代码的可维护性和扩展性。

## 过滤器架构

### 1. 抽象基类
- **AbstractPermissionFilter**: 权限过滤器的基础抽象类，提供通用的权限检查逻辑

### 2. 具体过滤器实现

#### WhiteListPermissionFilter (优先级: -200)
- **功能**: 处理白名单路径，不需要任何认证
- **处理逻辑**: 直接放行
- **配置**: 通过 `PERMISSIONS_WHITE_LIST` 权限类型配置

#### CookiePermissionFilter (优先级: -180)
- **功能**: 处理支持Cookie token的路径
- **处理逻辑**: 从Cookie中提取token并添加到请求头
- **配置**: 通过 `PERMISSIONS_COOKIE_LIST` 权限类型配置

#### LoginPermissionFilter (优先级: -150)
- **功能**: 处理只需要登录验证的路径
- **处理逻辑**: 验证token有效性，获取用户信息
- **配置**: 通过 `PERMISSIONS_LOGIN_LIST` 权限类型配置

#### RolePermissionFilter (优先级: -100)
- **功能**: 处理需要角色权限验证的路径
- **处理逻辑**: 验证token有效性，检查用户角色权限
- **配置**: 通过 `PERMISSIONS_AUTH_LIST` 权限类型配置

### 3. 工具类
- **UserAuthUtils**: 提供通用的用户信息获取和错误处理功能

### 4. 配置类
- **PermissionFilterConfiguration**: 统一管理所有权限过滤器的配置

## 执行流程

```
请求进入
    ↓
WhiteListPermissionFilter (检查白名单)
    ↓ (如果在白名单中，直接放行)
CookiePermissionFilter (处理Cookie token)
    ↓ (如果支持Cookie，提取token)
LoginPermissionFilter (检查登录权限)
    ↓ (如果需要登录验证，验证token)
RolePermissionFilter (检查角色权限)
    ↓ (如果需要角色权限，验证权限)
请求转发到后端服务
```

## 权限类型说明

### PERMISSIONS_WHITE_LIST
- 白名单权限，不需要任何认证
- 典型路径: `/auth/captcha/**`, `/auth/login/**`, `/*/static/**`

### PERMISSIONS_COOKIE_LIST
- 支持Cookie token的权限
- 允许从Cookie中获取token进行认证

### PERMISSIONS_LOGIN_LIST
- 登录权限，只需要验证用户已登录
- 不需要检查具体的角色权限

### PERMISSIONS_AUTH_LIST
- 角色权限，需要验证用户具有特定的角色权限
- 最严格的权限控制

## 配置示例

### 权限配置
```json
{
  "PERMISSIONS_WHITE_LIST": [
    {"url": "/auth/captcha/**", "method": "GET"},
    {"url": "/auth/login/**", "method": "POST"}
  ],
  "PERMISSIONS_LOGIN_LIST": [
    {"url": "/api/user/profile/**", "method": "GET"}
  ],
  "PERMISSIONS_AUTH_LIST": [
    {"url": "/api/admin/**", "method": "*"}
  ]
}
```

### 过滤器优先级
```java
// 优先级从高到低
WhiteListPermissionFilter: -200
CookiePermissionFilter: -180
LoginPermissionFilter: -150
RolePermissionFilter: -100
```

## 迁移指南

### 从旧架构迁移
1. 旧版本的 `JwtAuthenticationTokenFilter` 已被标记为 `@Deprecated`
2. 新的过滤器会自动接管权限验证
3. 建议逐步移除对旧过滤器的依赖

### 添加新的权限类型
1. 在 `CacheType` 枚举中添加新的权限类型
2. 创建对应的过滤器类继承 `AbstractPermissionFilter`
3. 在 `PermissionFilterConfiguration` 中注册新的过滤器
4. 配置相应的权限数据

## 优势

1. **职责分离**: 每种权限类型有独立的过滤器处理
2. **易于维护**: 代码结构清晰，便于理解和修改
3. **易于扩展**: 新增权限类型只需添加新的过滤器
4. **性能优化**: 通过优先级控制，避免不必要的权限检查
5. **代码复用**: 通用逻辑抽取到抽象类和工具类中

## 注意事项

1. 过滤器优先级很重要，确保按正确顺序执行
2. 权限配置需要正确设置，避免权限冲突
3. 缓存机制可以提高性能，但需要注意缓存一致性
4. 错误处理统一在 `UserAuthUtils` 中处理 