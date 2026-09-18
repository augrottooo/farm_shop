# 助农农产品电商平台

这是一个基于 Spring Boot 构建的前后端分离平台，实现农产品商品管理、SKU 规格、库存扣减、购物车、订单履约、优惠券和模拟物流核心闭环，主要实践集中在库存扣减、订单状态机、缓存和鉴权模块。

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
- 用户领券和优惠券列表
- 下单使用优惠券，记录原始金额、优惠金额和实付金额
- 订单支付入口、支付回调处理
- 用户取消订单、确认收货、再来一单、催单

### 管理端

- 员工登录、退出、分页查询和启停用
- 分类增删改查和启停用
- 商品新增、修改、分页查询、详情查询、上下架
- 商品 SKU 随商品保存
- SKU 绝对库存设置
- 优惠券模板新增、修改、分页、详情、启停用和删除
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

### 优惠券闭环

当前已实现优惠券基础业务：

1. 管理端维护优惠券模板，支持满减、折扣和无门槛三种类型。
2. 用户领取启用且在有效期内的优惠券，模板使用条件更新累加已领取数。
3. 用户券通过唯一键 `(user_id, coupon_template_id)` 防止重复领取同一模板。
4. 下单时服务端重新校验用户券归属、状态、有效期和使用门槛，并计算优惠金额。
5. 订单记录 `coupon_id`、`original_amount`、`discount_amount` 和实付 `amount`。
6. 下单成功后用户券由 `1未使用` 变为 `2已锁定`。
7. 当前模拟支付成功后，用户券由 `2已锁定` 变为 `3已使用`。
8. 用户取消、商家拒单/取消、超时取消时，锁定券退回 `1未使用`。

优惠券状态为：`1未使用、2已锁定、3已使用、4已过期`。过期券目前主要在查询时识别，不强制回写数据库。

## 技术亮点

### Redis + Lua 防止并发超卖

Redis Lua 将“读取库存、判断库存、扣减库存”放在同一个 Redis 原子脚本中，避免多个请求同时读取到同一份库存。Redis 负责高并发入口的快速拦截，MySQL 条件更新负责最终落库校验。

### 商品列表缓存

用户端 `GET /user/product/list` 已接入 Redis 缓存，缓存 key 为 `cache:product:list:category:{categoryId}`，当 `categoryId` 为空时使用 `cache:product:list:category:all`。

- 读链路采用 Cache Aside：先查 Redis，未命中再查 MySQL，回填缓存。
- 防穿透：查不到数据时缓存空列表，短 TTL。
- 防击穿：热点 key 回源时使用互斥锁，避免多个请求同时打库。
- 防雪崩：TTL 加随机扰动，避免批量同时失效。
- 管理端商品新增、修改、上下架后，会在事务提交后删除对应分类缓存和 `all` 缓存。

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

### 优惠券与订单金额

- 满减券按 `threshold_amount` 和 `discount_amount` 计算。
- 折扣券按 `discount_rate` 计算折扣金额。
- 无门槛券直接按 `discount_amount` 减免。
- 优惠券锁定使用 MySQL 条件更新，不并入库存 Redis Lua 脚本，避免为券额外维护 Redis 状态。

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

启动后可以使用 Knife4j 查看接口文档。

