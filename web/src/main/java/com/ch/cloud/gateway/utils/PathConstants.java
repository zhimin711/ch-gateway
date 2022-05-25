package com.ch.cloud.gateway.utils;

public interface PathConstants {

    String DOWNLOAD_PATTERN = "/**/download/**";
    String IMAGES_PATTERN   = "/**/images/**";
    String API_PATTERN      = "/*/v2/api-docs";

    String[] COOKIE_URLS = {"/*/v2/api-docs", "/**/download/**", "/**/images/**"};
}
