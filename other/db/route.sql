DROP TABLE IF EXISTS `bt_route`;
CREATE TABLE `bt_route`
(
  `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `path`         varchar(255)        DEFAULT NULL COMMENT '路径',
  `service_id`   varchar(255)        DEFAULT NULL COMMENT '服务ID',
  `url`          varchar(255)        DEFAULT NULL COMMENT '完整地址',
  `strip_prefix` tinyint(1) NOT NULL DEFAULT 1 COMMENT '忽略前缀',
  `retryable`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '0-不重试 1-重试',
  `status`       CHAR(1)    NOT NULL DEFAULT '1' COMMENT '状态:0-无效 1-有效',
  `description`  varchar(255)        DEFAULT NULL,
  `persist`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为保留数据:0-否 1-是',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  ROW_FORMAT = COMPACT COMMENT ='开放网关-路由';