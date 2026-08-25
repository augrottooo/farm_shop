# 当前接口文档

> 用途：记录当前源码中实际存在的 HTTP 接口，便于 IDEA、Knife4j 和 Postman 联调。本文不记录已删除的 `dish`、`setmeal`、`setmeal_dish`、`dish_flavor` 接口。

## 统一约定

- 服务地址示例：`http://localhost:8080`
- 返回结构使用项目的 `Result<T>`；分页接口使用 `Result<PageResult>`。
- 管理端接口需要携带管理端 JWT，用户端接口需要携带用户 JWT。
- 登录接口不需要 JWT。
- `product` 是商品主表，`product_sku` 是规格、价格和库存表。
- 当前订单状态：`1` 待付款、`2` 待发货、`3` 运输中、`4` 已签收、`5` 已完成、`6` 已取消。
- 当前支付状态：`0` 未支付、`1` 已支付、`2` 退款。

## 一、认证接口

### 1. 管理端登录

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/admin/employee/login` | 无 | `{"username":"admin","password":"123456"}` | `Result<EmployeeLoginVO>`，包含员工 id、用户名、姓名和 token |

### 2. 管理端退出

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/admin/employee/logout` | 管理端 JWT | 无 | `Result<String>` |

### 3. 用户微信登录

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/user/user/login` | 无 | `{"code":"微信登录临时code"}` | `Result<UserLoginVO>`，包含用户 id、openid 和 token |

`code` 不是固定字符串，需要由真实微信小程序调用登录能力获得。没有小程序环境时，不能用任意字符串替代真实 code 完成微信登录。

## 二、用户端商品与购物车

### 1. 查询分类

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/user/category/list` | Query：`type`，可选 | `Result<List<Category>>` |

### 2. 按分类查询商品

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/user/product/list` | Query：`categoryId` | `Result<List<Product>>` |

仅返回商品主表信息；选择具体规格时继续调用 SKU 查询接口。

### 3. 查询商品 SKU

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/user/product/sku/{productId}` | Path：`productId` | `Result<List<ProductSku>>` |

SKU 返回当前商品的 SKU 编码、名称、规格信息、价格、库存、上下架状态和排序等字段。

### 4. 添加购物车

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/user/shoppingCart/add` | 用户 JWT | `{"productId":1,"skuId":1}` | `Result` |

当前购物车按用户、商品和 SKU 查询；数量由后端累加。

### 5. 查看购物车

| 方法 | 路径 | 认证 | 返回 |
|---|---|---|---|
| GET | `/user/shoppingCart/list` | 用户 JWT | `Result<List<ShoppingCart>>` |

### 6. 减少一个购物车商品

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/user/shoppingCart/sub` | 用户 JWT | `{"productId":1,"skuId":1}` | `Result` |

### 7. 清空购物车

| 方法 | 路径 | 认证 | 返回 |
|---|---|---|---|
| DELETE | `/user/shoppingCart/clean` | 用户 JWT | `Result` |

## 三、管理端商品和库存

### 1. 新增商品及 SKU

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/admin/product` | 管理端 JWT | `ProductDTO` | `Result<String>` |

核心字段示例：

```json
{
  "categoryId": 1,
  "name": "洛川苹果",
  "subtitle": "产地直发",
  "sort": 1,
  "image": "https://example.com/apple.jpg",
  "description": "助农测试商品",
  "status": 1,
  "skus": [
    {
      "skuCode": "APPLE-5KG",
      "skuName": "5斤装",
      "specInfo": "{\"weight\":\"5斤\"}",
      "price": 39.90,
      "stock": 10,
      "lockedStock": 0,
      "status": 1,
      "sort": 1
    }
  ]
}
```

### 2. 商品分页查询

| 方法 | 路径 | 认证 | Query 参数 | 返回 |
|---|---|---|---|---|
| GET | `/admin/product/page` | 管理端 JWT | `page`、`pageSize`、`name`、`categoryId`、`status` | `Result<PageResult>` |

### 3. 查询商品详情

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| GET | `/admin/product/{id}` | 管理端 JWT | Path：商品 `id` | `Result<ProductVO>`，包含商品和 SKU 信息 |

### 4. 修改商品及 SKU

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| PUT | `/admin/product` | 管理端 JWT | `ProductDTO`，需要携带商品 id | `Result<String>` |

### 5. 商品上下架

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| POST | `/admin/product/status/{status}` | 管理端 JWT | Path：`status`；Query：`id` | `Result<String>` |

当前商品状态使用 `0` 下架、`1` 上架。注意：Controller 中商品 id 是普通请求参数，不是路径变量，请求示例为 `/admin/product/status/1?id=1`。

### 6. 管理端按分类查询商品

| 方法 | 路径 | 认证 | Query 参数 | 返回 |
|---|---|---|---|---|
| GET | `/admin/product/list` | 管理端 JWT | `categoryId` | `Result<List<Product>>` |

### 7. 设置 SKU 绝对库存

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| PUT | `/admin/product/sku/stock` | 管理端 JWT | `{"skuId":1,"stock":10}` | `Result<String>` |

该接口是设置目标库存，不是增加库存。执行后会更新 MySQL、同步 Redis，并写入 `business_type = 1` 的初始化流水。

## 四、用户端地址和订单

### 1. 地址簿

| 方法 | 路径 | 认证 | 参数/请求体 | 返回 |
|---|---|---|---|---|
| GET | `/user/addressBook/list` | 用户 JWT | 无 | `Result<List<AddressBook>>` |
| POST | `/user/addressBook` | 用户 JWT | `AddressBook` | `Result` |
| GET | `/user/addressBook/{id}` | 用户 JWT | Path：地址 id | `Result<AddressBook>` |
| PUT | `/user/addressBook` | 用户 JWT | `AddressBook` | `Result` |
| PUT | `/user/addressBook/default` | 用户 JWT | `{"id":1}` | `Result` |
| DELETE | `/user/addressBook?id=1` | 用户 JWT | Query：地址 id | `Result` |
| GET | `/user/addressBook/default` | 用户 JWT | 无 | `Result<AddressBook>` |

### 2. 提交订单

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/user/order/submit` | 用户 JWT | `OrdersSubmitDTO` | `Result<OrderSubmitVO>` |

当前请求体仍沿用原外卖字段，例如 `addressBookId`、`payMethod`、`remark`、`estimatedDeliveryTime`、`deliveryStatus`、`tablewareNumber`、`tablewareStatus`、`packAmount`、`amount`。商品金额由后端根据购物车中的 SKU 价格重新计算，不能以客户端 `amount` 作为最终金额。

下单时会执行 Redis Lua 预扣和 MySQL 条件扣减。库存不足会返回业务异常，订单数据库事务回滚。

### 3. 订单支付入口

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| PUT | `/user/order/payment` | 用户 JWT | `{"orderNumber":"订单号","payMethod":1}` | `Result<OrderPaymentVO>` |

当前 `OrderServiceImpl.payment` 中真实微信统一下单调用被注释，接口返回的是简化支付结果，并会更新订单支付状态和通过 WebSocket 推送来单提醒。不能把当前接口描述为已经完成真实生产收银台。

### 4. 查询历史订单

| 方法 | 路径 | 认证 | Query 参数 | 返回 |
|---|---|---|---|---|
| GET | `/user/order/historyOrders` | 用户 JWT | `page`、`pageSize`、`status` | `Result<PageResult>` |

### 5. 查询订单详情

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| GET | `/user/order/orderDetail/{id}` | 用户 JWT | Path：订单 id | `Result<OrderVO>` |

### 6. 用户取消订单

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| PUT | `/user/order/cancel/{id}` | 用户 JWT | Path：订单 id | `Result` |

取消时会根据订单明细回补 SKU 库存，同步 Redis，并写入 `RESTORE` 流水，再更新订单状态为已取消。

### 7. 再来一单和催单

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| POST | `/user/order/repetition/{id}` | 用户 JWT | Path：订单 id | `Result` |
| GET | `/user/order/reminder/{id}` | 用户 JWT | Path：订单 id | `Result` |

催单通过 WebSocket 向管理端推送消息。

## 五、订单状态机与模拟物流

### 1. 状态流转

| 当前状态 | 目标状态 | 触发 |
|---|---|---|
| 1 待付款 | 2 待发货 | 支付成功 |
| 1 待付款 | 6 已取消 | 用户取消 / 超时取消 |
| 2 待发货 | 3 运输中 | 管理端模拟发货 |
| 2 待发货 | 6 已取消 | 管理端取消 / 拒单 |
| 3 运输中 | 4 已签收 | 用户确认收货 |
| 4 已签收 | 5 已完成 | 订单完成 |

### 2. 模拟发货

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| POST | `/admin/logistics/ship` | 管理端 JWT | `{"orderId":1}` | `Result<String>` |

状态校验规则：

- 只允许 `待发货(2)` 的订单发货。
- 发货成功后，订单状态由 `2 -> 3`，同时插入一条 `logistics` 快照。
- `logistics` 只存展示信息，不再单独维护状态。
- 重复发货会因原状态不匹配被拒绝。

### 3. 物流查询

| 方法 | 路径 | 认证 | 参数 | 返回 |
|---|---|---|---|---|
| GET | `/user/logistics/{orderId}` | 用户 JWT | Path：订单 id | `Result<Logistics>` |

状态校验规则：

- 只能查询当前登录用户自己的订单。
- 如果订单不存在物流记录，返回业务异常。
- 返回的是物流展示快照，包括快递公司、单号、发货时间和收货人信息。

### 4. 用户确认收货

| 方法 | 路径 | 认证 | 请求体 | 返回 |
|---|---|---|---|---|
| PUT | `/user/order/receive` | 用户 JWT | `{"id":1}` | `Result` |

状态校验规则：

- 只允许 `运输中(3)` 的订单确认收货。
- 订单状态由 `3 -> 4`，物流表不更新。
- 重复确认收货会因原状态不匹配被拒绝。

### 5. 兼容入口

- `PUT /admin/order/confirm` 和 `PUT /admin/order/delivery/{id}` 仍然保留，当前内部已转为模拟发货流程。
- 建议面试和联调优先使用 `POST /admin/logistics/ship`。

## 六、管理端订单和基础接口

### 1. 订单管理

| 方法 | 路径 | 请求体/参数 | 返回 |
|---|---|---|---|
| GET | `/admin/order/conditionSearch` | Query：`OrdersPageQueryDTO` | `Result<PageResult>` |
| GET | `/admin/order/statistics` | 无 | `Result<OrderStatisticsVO>` |
| GET | `/admin/order/details/{id}` | Path：订单 id | `Result<OrderVO>` |
| PUT | `/admin/order/confirm` | `{"id":1}` | `Result` |
| PUT | `/admin/order/rejection` | `{"id":1,"rejectionReason":"..."}` | `Result` |
| PUT | `/admin/order/cancel` | `{"id":1,"cancelReason":"..."}` | `Result` |
| PUT | `/admin/order/delivery/{id}` | Path：订单 id | `Result` |
| PUT | `/admin/order/complete/{id}` | Path：订单 id | `Result` |

管理端取消或拒单同样会触发库存回补；已支付订单的原项目代码保留了微信退款调用入口，但完整退款记录和退款幂等流程尚未完成。

### 2. 分类管理

| 方法 | 路径 | 参数/请求体 | 返回 |
|---|---|---|---|
| POST | `/admin/category` | `CategoryDTO` | `Result<String>` |
| GET | `/admin/category/page` | `CategoryPageQueryDTO` | `Result<PageResult>` |
| DELETE | `/admin/category?id=1` | Query：分类 id | `Result<String>` |
| PUT | `/admin/category` | `CategoryDTO` | `Result<String>` |
| POST | `/admin/category/status/{status}` | Path：状态；Query：`id` | `Result<String>` |
| GET | `/admin/category/list` | Query：`type` | `Result<List<Category>>` |

分类实体仍保留原项目的 `type` 字段，商品改造阶段复用分类表，不新增租户字段。

### 3. 店铺和文件

| 方法 | 路径 | 参数/请求体 | 返回 |
|---|---|---|---|
| PUT | `/admin/shop/{status}` | Path：`status` | `Result` |
| GET | `/admin/shop/status` | 无 | `Result<Integer>` |
| POST | `/admin/common/upload` | multipart：`file` | `Result<String>`，返回 OSS 文件地址 |

## 七、支付回调和 WebSocket

### 1. 微信支付成功回调

| 方法 | 路径 | 认证 | 请求 | 返回 |
|---|---|---|---|---|
| 由框架映射 | `/notify/paySuccess` | 第三方回调 | 微信支付通知 JSON | HTTP JSON：`{"code":"SUCCESS","message":"SUCCESS"}` |

当前回调会读取通知、解密资源中的商户订单号和微信交易号，调用订单服务更新支付状态。回调验签、幂等和主动查单仍需在真实支付接入时继续完善。

### 2. WebSocket

| 类型 | 地址 | 用途 |
|---|---|---|
| WebSocket | `/ws/{sid}` | 管理端接收来单提醒、用户催单提醒 |

## 八、库存相关内部约定

| Redis Key | 含义 |
|---|---|
| `stock:sku:{skuId}` | SKU 当前用于预扣的 Redis 库存 |

| 库存流水类型 | 含义 |
|---|---|
| `1` | 初始化/设置绝对库存 |
| `2` | 订单扣减 |
| `3` | 订单取消、拒单或超时回补 |

库存扣减脚本位于 `sky-server/src/main/resources/lua/deductStock.lua`。Redis 预扣成功但下单失败时使用 `INCRBY` 补偿；Redis 异常时当前实现允许降级到 MySQL 条件扣减。

## 九、当前未提供的接口

- `/admin/dish/**`、`/admin/setmeal/**`
- `/user/dish/**`、`/user/setmeal/**`
- 优惠券领取、锁券、用券、退券接口
- 完整退款申请、审核、原路退款和退款记录接口
- 真实物流公司对接接口
- 秒杀活动接口
- 溯源、农户档案、SaaS 多租户和区块链存证接口
