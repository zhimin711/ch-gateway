package com.ch.cloud.gateway.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {

    /**
     * 用户名
     */
    private String username;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 角色ID
     */
    private Long roleId;
    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 过期时间
     */
    private Long expireAt;
}
