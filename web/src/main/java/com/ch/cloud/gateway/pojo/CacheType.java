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
    PERMISSIONS_MAP("gateway:permissions","permissions"),
    PERMISSIONS_WHITE_LIST("gateway:permission:whitelist","whitelist"),
    PERMISSIONS_LOGIN_LIST("gateway:permission:login","login"),
    PERMISSIONS_COOKIE_LIST("gateway:permission:cookie","cookie"),
    PERMISSIONS_AUTH_LIST("gateway:permission:auth",""),
    GATEWAY_TOKEN("gateway:token", "token"),
    GATEWAY_USER("gateway:user","user");

    private final String key;
    private final String code;

    CacheType(String key,String code) {
        this.key = key;
        this.code = code;
    }

    public String getKey(String... args) {
        if (args == null || args.length == 0) {
            return key;
        }
        return key + Separator.SECURITY + StringUtilsV2.linkStr(Separator.SECURITY, args);
    }

    public String getCode() {
        return code;
    }
}
