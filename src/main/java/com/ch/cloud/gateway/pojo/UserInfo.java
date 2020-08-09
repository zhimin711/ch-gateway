package com.ch.cloud.gateway.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {

    private String username;
    private Long userId;
    private Long roleId;

    private Long expireAt;
}
