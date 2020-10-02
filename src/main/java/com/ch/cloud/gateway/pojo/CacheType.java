package com.ch.cloud.gateway.pojo;

import com.ch.Constants;
import com.ch.utils.StringExtUtils;

/**
 * desc:
 *
 * @author zhimi
 * @date 2020/10/2 14:47
 */
public enum CacheType {
    PERMISSIONS_WHITE_LIST("gateway:permission:whitelist"),
    PERMISSIONS_LOGIN_LIST("gateway:permission:login"),
    PERMISSIONS_AUTH_LIST("gateway:permission:auth");

    private final String key;

    CacheType(String key) {
        this.key = key;
    }

    public String getKey(String... args) {
        if (args == null) {
            return key;
        }
        return key + Constants.SECURITY_SEPARATOR + StringExtUtils.linkStr(Constants.SECURITY_SEPARATOR, args);
    }
}
