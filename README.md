# 助农农产品电商平台

这是一个基于 Spring Boot 的单体多模块农产品销售平台。项目由原外卖订餐系统演进而来，商品模型已经从 `dish/setmeal` 切换为 `product/product_sku`，当前重点验证商品浏览、购物车、下单和库存一致性闭环。

> 文档以当前源码为准。当前订单状态已调整为“待付款、待发货、运输中、已签收、已完成、已取消”，物流为模拟履约，优惠券和退款业务仍未完成。

## 项目定位

平台面向助农采购场景，为用户提供农产品浏览、规格选择、加购和下单能力，为管理端提供商品、SKU、库存、分类和订单管理能力。项目的演进重点不是简单替换名称，而是把原来“无库存的餐饮商品”改造成“有 SKU、有库存、可记录库存变动”的商品模型。

## 技术栈

- Java 17
- Spring Boot 2.7.3
- MyBatis
- MySQL
- Redis
- Redis Lua
- JWT
- WebSocket
- PageHelper
- 阿里云 OSS
- 微信登录/支付相关工具类

当前没有引入 Caffeine、Redisson、RocketMQ，也没有实现多级缓存或消息队列方案。

## 模块结构

```text
sky-take-out
├── sky-common       通用常量、异常、JWT、上下文、工具类
├── sky-pojo         DTO、Entity、VO
└── sky-server       Controller、Service、Mapper、配置、拦截器、任务、WebSocket
```

项目采用 Controller -> Service -> Mapper 的分层结构。用户端和管理端通过 JWT 拦截器区分访问权限，当前登录用户/员工信息通过请求上下文传递给业务层。

## 当前核心功能

### 用户端

- 微信登录并生成用户 JWT
- 查询分类和上架商品
- 查询商品 SKU 和规格价格
- 按商品和 SKU 加入购物车、减少商品、清空购物车
- 地址簿维护
- 提交订单、订单详情、历史订单
- 订单支付入口、支付回调处理
- 用户取消订单、再来一单、催单

### 管理端

- 员工登录、退出、分页查询和启停用
- 分类增删改查和启停用
- 商品新增、修改、分页查询、详情查询、上下架
- 商品 SKU 随商品保存
- SKU 绝对库存设置
- 订单查询、发货、确认收货、取消、完成
- 店铺营业状态设置
- OSS 文件上传

### 订单状态与模拟物流

订单状态流转为：

`1 待付款 -> 2 待发货 -> 3 运输中 -> 4 已签收 -> 5 已完成 -> 6 已取消`

当前 `orders.status` 是唯一权威状态，`logistics` 只保存发货时的展示信息，不再独立维护状态机。

新增接口：

- `POST /admin/logistics/ship`
- `GET /user/logistics/{orderId}`
- `PUT /user/order/receive`

### 库存闭环

当前下单流程已经接入以下逻辑：

1. 优先使用 Redis Lua 脚本对 `stock:sku:{skuId}` 做原子预扣。
2. Redis 不可用时降级为数据库扣减。
3. 订单插入后，MySQL 使用 `stock = stock - count` 且 `stock >= count` 的条件更新兜底。
4. 数据库扣减成功后写入 `stock_log`。
5. 用户取消、管理端取消/拒单、超时未支付时，回补 MySQL、同步 Redis，并写入回补流水。
6. 管理端设置绝对库存时写入 `INIT` 流水并同步 Redis。

库存流水中的 `business_type` 当前约定为：`1` 初始化、`2` 扣减、`3` 回补。

## 技术亮点

### Redis + Lua 防止并发超卖

Redis Lua 将“读取库存、判断库存、扣减库存”放在同一个 Redis 原子脚本中，避免多个请求同时读取到同一份库存。Redis 负责高并发入口的快速拦截，MySQL 条件更新负责最终落库校验。

### MySQL 条件更新兜底

库存扣减不是先查再改，而是直接执行：

```sql
UPDATE product_sku
SET stock = stock - #{count}
WHERE id = #{skuId}
  AND stock >= #{count};
```

通过影响行数判断是否扣减成功，避免并发请求把库存扣成负数。

### 取消与超时回补

订单取消和定时任务取消会复用 `StockService.restoreStockByOrderId`，根据订单明细逐个 SKU 回补库存。库存回补与订单状态更新位于同一业务事务中，Redis 同步失败时记录日志，数据库仍作为库存最终依据。

### 模拟物流

- 管理端发货时创建一条物流快照，快递公司固定为模拟快递。
- 物流表只存展示信息，后续不单独推进物流状态。
- 用户确认收货时只更新订单状态，不改物流表。
- 这样可以避免订单状态和物流状态出现两套口径。

### WebSocket 和定时任务

- WebSocket 用于支付成功后的来单提醒和用户催单提醒。
- `OrderTask` 每分钟扫描超过 15 分钟仍处于待付款的订单并自动取消。

## 当前数据库模型

当前商品和库存相关核心表包括：

- `product`：商品主表
- `product_sku`：商品规格和库存
- `stock_log`：库存初始化、扣减、回补流水
- `category`：商品分类
- `shopping_cart`：用户购物车，关联 `product_id` 和 `sku_id`
- `orders`：订单主表
- `order_detail`：订单明细，关联 `product_id` 和 `sku_id`
- `user`、`employee`、`address_book`：用户、员工和收货地址

数据库脚本位于 `docs/sql/`。新增商品模型使用 `docs/sql/新增商品模型.sql`，测试商品和 SKU 使用 `docs/sql/测试数据.sql`。

## 快速开始

### 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis 6.x 或更高版本

### 初始化数据库

1. 创建数据库 `sky_take_out`。
2. 执行原项目基础表脚本。
3. 执行 `docs/sql/新增商品模型.sql`。
4. 按需执行 `docs/sql/测试数据.sql`。
5. 确认 MySQL 和 Redis 已启动，并检查项目配置中的连接信息。

也可以直接在 IntelliJ IDEA 的 Database 工具中打开 SQL 文件，选择 `sky_take_out` 数据库后执行。Windows 命令行重定向执行失败时，优先使用 IDEA 数据库控制台，避免命令行没有加入 MySQL 的 `bin` 目录或密码输入方式不匹配。

### 启动项目

在 IDEA 中运行 `com.sky.SkyApplication`。默认 HTTP 端口为 `8080`，具体以当前环境配置为准。

启动后可以使用 Knife4j 查看接口文档，也可以参考 `docs/API.md` 使用接口。

## 当前未实现与项目边界

- 溯源：`product_batch`、`farmer` 及相关业务不在当前项目范围内。
- SaaS 多租户：不增加 `merchant_id`，不实现租户拦截器。
- 微信订阅消息：不新增订阅消息，保留原 WebSocket 通知。
- 区块链存证：不实现。
- 优惠券：数据库表已预留，但用户领券、下单锁券、支付用券、退款退券流程未实现。
- 退款：部分原外卖拒单/取消代码保留微信退款调用痕迹，但没有形成完整退款记录和回补闭环。
- 物流：已实现模拟发货、物流查询和确认收货，不对接真实物流公司。
- 秒杀：没有秒杀活动模型和独立秒杀链路。

## 文档索引

- [API 接口文档](docs/API.md)
- [面试讲解稿](docs/面试讲解稿.md)
- [新增商品模型 SQL](docs/sql/新增商品模型.sql)
- [测试数据 SQL](docs/sql/测试数据.sql)
- [溯源清理 SQL](docs/sql/清理溯源.sql)
