package com.ch.cloud.gateway.cli;

import com.ch.cloud.client.UpmsConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * desc:用户微服务
 *
 * @author zhimin
 * @date 2019/4/15 12:41 PM
 */

@FeignClient(name = UpmsConstants.NAME)
public interface UpmsClientService extends com.ch.cloud.client.UpmsClientService {

    Object login(String username, String password);
}

