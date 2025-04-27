package com.ch.cloud.gateway.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * decs:
 *
 * @author 01370603
 * @since 2019/12/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRoute {

    private Long id;

    private String serviceId;

    private String uri;

    private String predicates;

    private String filters;

    private String order;

    private String creatorId;

    private Date createDate;

    private String updateId;

    private Date updateDate;

    private String remarks;

    private String delFlag;

}
