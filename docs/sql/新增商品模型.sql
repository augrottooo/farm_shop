-- 商品模型替换：先建新，再切代码，最后删旧
-- 执行顺序建议：
-- 1. 先执行本文件
-- 2. 再部署新代码
-- 3. 验证无旧表引用后执行 删除旧表.sql

CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '分类id',
  `name` varchar(64) NOT NULL COMMENT '商品名称',
  `subtitle` varchar(128) DEFAULT NULL COMMENT '商品副标题',
  `sort` int DEFAULT 0 COMMENT '排序',
  `image` varchar(255) DEFAULT NULL COMMENT '主图',
  `description` varchar(500) DEFAULT NULL COMMENT '商品描述',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态 0下架 1上架',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_product_category_status` (`category_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表';

CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_code` varchar(64) DEFAULT NULL COMMENT 'SKU编码',
  `sku_name` varchar(128) NOT NULL COMMENT 'SKU名称',
  `spec_info` varchar(500) DEFAULT NULL COMMENT '规格信息(JSON字符串)',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '可售库存',
  `locked_stock` int NOT NULL DEFAULT 0 COMMENT '锁定库存',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态 0下架 1上架',
  `sort` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_product_sku_product` (`product_id`),
  KEY `idx_product_sku_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `coupon_name` varchar(64) NOT NULL COMMENT '优惠券名称',
  `coupon_type` int NOT NULL COMMENT '1满减 2折扣 3无门槛',
  `threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '门槛金额',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金额',
  `discount_rate` decimal(5,2) DEFAULT NULL COMMENT '折扣率',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总发行量',
  `issue_count` int NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `receive_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  `valid_days` int DEFAULT NULL COMMENT '领券后有效天数',
  `start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `coupon_template_id` bigint NOT NULL COMMENT '优惠券模板id',
  `coupon_code` varchar(64) NOT NULL COMMENT '券码',
  `coupon_status` int NOT NULL DEFAULT 0 COMMENT '0未用 1已用 2已过期 3冻结',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_coupon_user_status` (`user_id`, `coupon_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领券表';

CREATE TABLE IF NOT EXISTS `stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `business_type` int NOT NULL COMMENT '1初始化 2扣减 3回补',
  `biz_id` bigint DEFAULT NULL COMMENT '业务id',
  `change_count` int NOT NULL COMMENT '变动数量',
  `before_stock` int DEFAULT NULL COMMENT '变更前库存',
  `after_stock` int DEFAULT NULL COMMENT '变更后库存',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_stock_log_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

CREATE TABLE IF NOT EXISTS `refund_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `refund_no` varchar(64) NOT NULL COMMENT '退款单号',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `refund_status` int NOT NULL DEFAULT 0 COMMENT '0待处理 1成功 2失败',
  `reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
  `channel` varchar(32) DEFAULT NULL COMMENT '退款渠道',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '支付/退款流水号',
  `applied_at` datetime DEFAULT NULL COMMENT '申请时间',
  `success_at` datetime DEFAULT NULL COMMENT '成功时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_refund_record_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

CREATE TABLE IF NOT EXISTS `logistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `carrier_code` varchar(32) DEFAULT NULL COMMENT '物流公司编码',
  `carrier_name` varchar(64) DEFAULT NULL COMMENT '物流公司名称',
  `tracking_no` varchar(64) DEFAULT NULL COMMENT '快递单号',
  `ship_status` int NOT NULL DEFAULT 0 COMMENT '0待发货 1已发货 2运输中 3已签收 4异常',
  `shipped_at` datetime DEFAULT NULL COMMENT '发货时间',
  `delivered_at` datetime DEFAULT NULL COMMENT '签收时间',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(32) DEFAULT NULL COMMENT '收货电话',
  `receiver_address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_logistics_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

ALTER TABLE `shopping_cart`
  ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '商品id' AFTER `user_id`,
  ADD COLUMN `sku_id` bigint DEFAULT NULL COMMENT '商品SKU id' AFTER `product_id`;

ALTER TABLE `order_detail`
  ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '商品id' AFTER `order_id`,
  ADD COLUMN `sku_id` bigint DEFAULT NULL COMMENT '商品SKU id' AFTER `product_id`;
