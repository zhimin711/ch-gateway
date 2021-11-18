package com.ch.cloud.gateway.pojo;

import com.ch.Constants;
import com.ch.Separator;
import com.ch.utils.StringUtilsV2;

/**
 * desc:
 *
 * @author zhimi
 * @date 2020/10/2 14:47
 */
public enum CacheType {
    PERMISSIONS_WHITE_LIST("gateway:permission:whitelist"),
    PERMISSIONS_LOGIN_LIST("gateway:permission:login"),
    PERMISSIONS_COOKIE_LIST("gateway:permission:cookie"),
    PERMISSIONS_AUTH_LIST("gateway:permission:auth");

    private final String key;

    CacheType(String key) {
        this.key = key;
    }

    public String getKey(String... args) {
        if (args == null || args.length == 0) {
            return key;
        }
        return key + Separator.SECURITY + StringUtilsV2.linkStr(Separator.SECURITY, args);
    }
}
