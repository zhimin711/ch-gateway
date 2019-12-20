package com.ch.cloud.gateway.controller;

import com.ch.cloud.gateway.conf.SwaggerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.swagger.web.SwaggerResource;

import java.util.List;

/**
 * decs:
 *
 * @author 01370603
 * @date 2019/11/29
 */
//@RestController
@RequestMapping("/swagger-resources")
public class SwaggerResourceController {

    @Autowired
    SwaggerConfiguration swaggerResourceProvider;

    @RequestMapping
    public ResponseEntity<List<SwaggerResource>> swaggerResources() {
        return new ResponseEntity<>(swaggerResourceProvider.get(), HttpStatus.OK);
    }


}
