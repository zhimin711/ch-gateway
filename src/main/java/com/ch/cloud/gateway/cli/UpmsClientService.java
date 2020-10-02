package com.ch.cloud.gateway.cli;

import com.ch.cloud.client.UpmsConstants;
import com.ch.cloud.client.dto.PermissionDto;
import com.ch.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * desc:用户微服务
 *
 * @author zhimin
 * @date 2019/4/15 12:41 PM
 */

@FeignClient(name = UpmsConstants.NAME)
public interface UpmsClientService extends com.ch.cloud.client.UpmsClientService {

    @GetMapping({"permission/whitelist"})
    Result<PermissionDto> findWhitelistPermissions();

}

