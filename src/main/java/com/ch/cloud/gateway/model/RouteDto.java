package com.ch.cloud.gateway.model;

import java.io.Serializable;

/**
 * RouteDto ��չ����
 * 
 * @author zhimi
 * @date Mon May 13 22:36:23 CST 2019
 */
public class RouteDto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * ����
     */
    private Long id;

    /**
     * ·��
     */
    private String path;

    /**
     * ����ID
     */
    private String serviceId;

    /**
     * ������ַ
     */
    private String url;

    /**
     * ����ǰ׺
     */
    private Boolean stripPrefix;

    /**
     * 0-������ 1-����
     */
    private Boolean retryable;

    /**
     * ״̬:0-��Ч 1-��Ч
     */
    private String status;

    /**
     * 
     */
    private String description;

    /**
     * �Ƿ�Ϊ��������:0-�� 1-��
     */
    private Boolean persist;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public void setStripPrefix(Boolean stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public Boolean getStripPrefix() {
        return this.stripPrefix;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Boolean getRetryable() {
        return this.retryable;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setPersist(Boolean persist) {
        this.persist = persist;
    }

    public Boolean getPersist() {
        return this.persist;
    }
}