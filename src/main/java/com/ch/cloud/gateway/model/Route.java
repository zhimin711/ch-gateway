package com.ch.cloud.gateway.model;

import javax.persistence.*;

@Table(name = "bt_route")
public class Route {
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 路径
     */
    private String path;

    /**
     * 服务ID
     */
    @Column(name = "service_id")
    private String serviceId;

    /**
     * 完整地址
     */
    private String url;

    /**
     * 忽略前缀
     */
    @Column(name = "strip_prefix")
    private Boolean stripPrefix;

    /**
     * 0-不重试 1-重试
     */
    private Boolean retryable;

    /**
     * 状态:0-无效 1-有效
     */
    private String status;

    private String description;

    /**
     * 是否为保留数据:0-否 1-是
     */
    private Boolean persist;

    /**
     * 获取主键
     *
     * @return id - 主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键
     *
     * @param id 主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取路径
     *
     * @return path - 路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置路径
     *
     * @param path 路径
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取服务ID
     *
     * @return service_id - 服务ID
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * 设置服务ID
     *
     * @param serviceId 服务ID
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * 获取完整地址
     *
     * @return url - 完整地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置完整地址
     *
     * @param url 完整地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取忽略前缀
     *
     * @return strip_prefix - 忽略前缀
     */
    public Boolean getStripPrefix() {
        return stripPrefix;
    }

    /**
     * 设置忽略前缀
     *
     * @param stripPrefix 忽略前缀
     */
    public void setStripPrefix(Boolean stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    /**
     * 获取0-不重试 1-重试
     *
     * @return retryable - 0-不重试 1-重试
     */
    public Boolean getRetryable() {
        return retryable;
    }

    /**
     * 设置0-不重试 1-重试
     *
     * @param retryable 0-不重试 1-重试
     */
    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    /**
     * 获取状态:0-无效 1-有效
     *
     * @return status - 状态:0-无效 1-有效
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态:0-无效 1-有效
     *
     * @param status 状态:0-无效 1-有效
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取是否为保留数据:0-否 1-是
     *
     * @return persist - 是否为保留数据:0-否 1-是
     */
    public Boolean getPersist() {
        return persist;
    }

    /**
     * 设置是否为保留数据:0-否 1-是
     *
     * @param persist 是否为保留数据:0-否 1-是
     */
    public void setPersist(Boolean persist) {
        this.persist = persist;
    }
}