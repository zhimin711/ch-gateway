package com.ch.cloud.gateway.pojo;

import com.ch.Separator;
import com.ch.core.utils.StrUtil;
import lombok.Getter;

/**
 * desc:
 *
 * @author zhimi
 * @since2020/10/2 14:47
 */
@Getter
public enum CacheType {
    PERMISSIONS_MAP("gateway:permissions", "permissions"),
    PERMISSIONS_WHITE_LIST("gateway:permission:whitelist", "whitelist"),
    PERMISSIONS_LOGIN_LIST("gateway:permission:login", "login"),
    PERMISSIONS_COOKIE_LIST("gateway:permission:cookie", "cookie"),
    PERMISSIONS_AUTH_LIST("gateway:permission:auth", ""),
    PERMISSIONS_TEMP_LIST("gateway:permission:temp", "temp"),
    GATEWAY_TOKEN("gateway:token", "token"),
    GATEWAY_USER("gateway:user", "user");

    private final String key;

    private final String code;

    CacheType(String key, String code) {
        this.key = key;
        this.code = code;
    }

    public String key(String... args) {
        if (args == null || args.length == 0) {
            return key;
        }
        return key + Separator.SECURITY + StrUtil.linkStr(Separator.SECURITY, args);
    }

}
