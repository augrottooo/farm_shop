package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ProductBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.enumeration.OrderStatus;
import com.sky.service.OrderService;
import com.sky.service.LogisticsService;
import com.sky.service.StockService;
import com.sky.state.OrderStateMachine;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private CouponTemplateMapper couponTemplateMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private StockService stockService;
    @Autowired
    private LogisticsService logisticsService;
    @Autowired
    private OrderStateMachine orderStateMachine;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        //1. 处理各种业务异常（地址簿为空、购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null || !userId.equals(addressBook.getUserId())){
            // 抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询当前用户的购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList == null || shoppingCartList.size() == 0){
            // 抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        List<OrderDetail> orderDetailList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        // 先按服务端 SKU 价格校验并计算总价，不能信任客户端传入的金额。
        List<ShoppingCart> redisReservedCartList = new ArrayList<>();
        boolean redisAvailable = true;

        CouponPricing couponPricing;
        try {
            for (ShoppingCart cart : shoppingCartList) {
                if (cart.getSkuId() == null || cart.getProductId() == null) {
                    throw new ProductBusinessException("购物车商品缺少SKU信息");
                }

                ProductSku sku = productSkuMapper.getById(cart.getSkuId());
                Product product = productMapper.getById(cart.getProductId());
                if (sku == null || product == null
                        || !product.getId().equals(sku.getProductId())
                        || !StatusConstant.ENABLE.equals(product.getStatus())
                        || !StatusConstant.ENABLE.equals(sku.getStatus())) {
                    throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
                }

                if (redisAvailable) {
                    try {
                        boolean reserved = stockService.reserveStock(cart.getSkuId(), cart.getNumber());
                        if (reserved) {
                            redisReservedCartList.add(cart);
                        } else {
                            redisAvailable = false;
                            restoreRedisReservations(redisReservedCartList);
                        }
                    } catch (ProductBusinessException ex) {
                        restoreRedisReservations(redisReservedCartList);
                        throw ex;
                    }
                }

                OrderDetail orderDetail = new OrderDetail();
                //orderDetail.setOrderId(orders.getId());
                orderDetail.setProductId(product.getId());
                orderDetail.setSkuId(sku.getId());
                orderDetail.setName(product.getName() + "-" + sku.getSkuName());
                orderDetail.setImage(product.getImage());
                orderDetail.setNumber(cart.getNumber());
                orderDetail.setAmount(sku.getPrice().multiply(BigDecimal.valueOf(cart.getNumber())));
                totalAmount = totalAmount.add(sku.getPrice().multiply(BigDecimal.valueOf(cart.getNumber())));
                orderDetailList.add(orderDetail);
            }

            couponPricing = calculateCouponPricing(
                    ordersSubmitDTO.getCouponId(),
                    userId,
                    totalAmount);
        } catch (RuntimeException ex) {
            restoreRedisReservations(redisReservedCartList);
            throw ex;
        }

        //2. 向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setCouponId(couponPricing.couponId);
        orders.setOriginalAmount(totalAmount);
        orders.setDiscountAmount(couponPricing.discountAmount);
        orders.setAmount(couponPricing.payableAmount);
        try {
            orderMapper.insert(orders);

            orderDetailList.forEach(orderDetail -> orderDetail.setOrderId(orders.getId()));

            if (couponPricing.couponId != null) {
                int lockedRows = userCouponMapper.lockById(
                        couponPricing.couponId,
                        userId,
                        orders.getId());
                if (lockedRows == 0) {
                    throw new ProductBusinessException(MessageConstant.COUPON_LOCK_FAILED);
                }
            }

            // 订单头先落库拿到 orderId；库存扣减和后续写入仍在本方法事务内。
            for (ShoppingCart cart : shoppingCartList) {
                stockService.deductStock(cart.getSkuId(), cart.getNumber(), orders.getId());
            }

            // Redis 预扣成功才会走到这里；如果前面 Redis 降级失败，直接走 DB 扣减，不再保留 Redis 预扣痕迹。
            if (!redisAvailable) {
                log.info("Redis 库存预扣降级为数据库扣减，订单号: {}", orders.getNumber());
            }

            orderDetailMapper.insertBatch(orderDetailList);

            //4. 清空当前用户的购物车数据
            shoppingCartMapper.deleteByUserId(userId);
        } catch (Exception ex) {
            if (redisAvailable) {
                restoreRedisReservations(redisReservedCartList);
            }
            throw ex;
        }

        //5. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    private void restoreRedisReservations(List<ShoppingCart> reservedCartList) {
        if (reservedCartList == null || reservedCartList.isEmpty()) {
            return;
        }
        reservedCartList.forEach(item ->
                stockService.restoreReservedStock(item.getSkuId(), item.getNumber()));
        reservedCartList.clear();
    }

    private CouponPricing calculateCouponPricing(Long couponId,
                                                 Long userId,
                                                 BigDecimal originalAmount) {
        if (couponId == null) {
            return new CouponPricing(null, BigDecimal.ZERO, originalAmount);
        }

        UserCoupon userCoupon = userCouponMapper.getById(couponId);
        if (userCoupon == null) {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_FOUND);
        }
        if (!userId.equals(userCoupon.getUserId())) {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_BELONG_TO_USER);
        }
        if (!UserCoupon.UNUSED.equals(userCoupon.getCouponStatus())) {
            throw new ProductBusinessException(MessageConstant.COUPON_LOCK_FAILED);
        }

        CouponTemplate template = couponTemplateMapper.getById(userCoupon.getCouponTemplateId());
        if (template == null) {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!StatusConstant.ENABLE.equals(template.getStatus())
                || template.getStartTime() == null
                || template.getEndTime() == null
                || now.isBefore(template.getStartTime())
                || now.isAfter(template.getEndTime())
                || (userCoupon.getExpireTime() != null && now.isAfter(userCoupon.getExpireTime()))) {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_AVAILABLE);
        }

        BigDecimal discountAmount;
        if (Integer.valueOf(1).equals(template.getCouponType())) {
            if (template.getThresholdAmount() == null
                    || originalAmount.compareTo(template.getThresholdAmount()) < 0) {
                throw new ProductBusinessException(MessageConstant.COUPON_NOT_MEET_THRESHOLD);
            }
            discountAmount = template.getDiscountAmount();
        } else if (Integer.valueOf(2).equals(template.getCouponType())) {
            if (template.getDiscountRate() == null) {
                throw new ProductBusinessException(MessageConstant.COUPON_NOT_AVAILABLE);
            }
            discountAmount = originalAmount
                    .multiply(BigDecimal.ONE.subtract(template.getDiscountRate()))
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (Integer.valueOf(3).equals(template.getCouponType())) {
            discountAmount = template.getDiscountAmount();
        } else {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_AVAILABLE);
        }

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductBusinessException(MessageConstant.COUPON_NOT_AVAILABLE);
        }
        if (discountAmount.compareTo(originalAmount) > 0) {
            discountAmount = originalAmount;
        }

        BigDecimal payableAmount = originalAmount
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);
        return new CouponPricing(couponId, discountAmount, payableAmount);
    }

    private static class CouponPricing {

        private final Long couponId;
        private final BigDecimal discountAmount;
        private final BigDecimal payableAmount;

        private CouponPricing(Long couponId,
                              BigDecimal discountAmount,
                              BigDecimal payableAmount) {
            this.couponId = couponId;
            this.discountAmount = discountAmount;
            this.payableAmount = payableAmount;
        }
    }

    private void useCouponAfterPayment(Orders orders) {
        if (orders.getCouponId() == null) {
            return;
        }

        int affectedRows = userCouponMapper.useByOrderId(orders.getId());
        if (affectedRows == 0) {
            throw new ProductBusinessException(MessageConstant.COUPON_USE_FAILED);
        }
    }

    private void unlockCouponByOrderId(Long orderId) {
        userCouponMapper.unlockByOrderId(orderId);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Transactional
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

    /*    //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
*/

        // 根据订单编号查询订单，获取orderId
        Orders orders = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Long orderId = orders.getId();

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        Integer orderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer orderStatus = OrderStatus.WAITING_SHIP.getCode();  //订单状态，待发货

        //发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();
        int affectedRows = orderMapper.updateStatus(
                orderStatus,
                orderPaidStatus,
                check_out_time,
                orderId,
                Orders.PENDING_PAYMENT);
        if (affectedRows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        useCouponAfterPayment(orders);

        // =====================【新增WebSocket推送 放在方法末尾】=====================
        Map map = new HashMap();
        map.put("type",1);// 1表示来单提醒 2表示客户催单
        map.put("orderId",orders.getId());
        map.put("content","订单号：" + orders.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        // ======================================================================


        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Transactional
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        int affectedRows = orderMapper.updateStatus(
                OrderStatus.WAITING_SHIP.getCode(),
                Orders.PAID,
                LocalDateTime.now(),
                ordersDB.getId(),
                Orders.PENDING_PAYMENT);
        if (affectedRows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        useCouponAfterPayment(ordersDB);

    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Transactional
    public void userCancelById(Long id) throws Exception {
        Orders orders = getOrderOrThrow(id);
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        transitionOrder(orders, OrderStatus.CANCELLED);

        // 只有状态条件更新成功后才回补，避免并发重复取消导致重复回补。
        unlockCouponByOrderId(id);
        stockService.restoreStockByOrderId(id);

        Orders update = Orders.builder()
                .id(id)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(update);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 兼容旧管理端入口：确认接单统一转为模拟发货，不能绕过物流记录。
        logisticsService.ship(ordersConfirmDTO.getId());
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders ordersDB = getOrderOrThrow(ordersRejectionDTO.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        transitionOrder(ordersDB, OrderStatus.CANCELLED);

        // 拒单进入已取消状态，需要回补已扣减库存。
        unlockCouponByOrderId(ordersDB.getId());
        stockService.restoreStockByOrderId(ordersDB.getId());

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = getOrderOrThrow(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        transitionOrder(ordersDB, OrderStatus.CANCELLED);

        // 回补库存和库存流水
        unlockCouponByOrderId(ordersCancelDTO.getId());
        stockService.restoreStockByOrderId(ordersCancelDTO.getId());

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Transactional
    public void cancelTimeoutOrder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            return;
        }

        int affectedRows = orderMapper.updateStatusByIdAndStatus(
                id,
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.PENDING_PAYMENT.getCode());
        if (affectedRows == 0) {
            return;
        }

        unlockCouponByOrderId(id);
        stockService.restoreStockByOrderId(id);

        Orders update = Orders.builder()
                .id(id)
                .cancelReason("订单超时，自动取消")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(update);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 兼容旧入口，实际发货逻辑统一由物流服务完成。
        logisticsService.ship(id);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        Orders orders = getOrderOrThrow(id);
        transitionOrder(orders, OrderStatus.COMPLETED);
    }

    /**
     * 用户确认收货：运输中 -> 已签收。
     */
    @Transactional
    public void receive(Long id) {
        Orders orders = getOrderOrThrow(id);
        if (!BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        transitionOrder(orders, OrderStatus.SIGNED);
    }

    private Orders getOrderOrThrow(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    private void transitionOrder(Orders orders, OrderStatus targetStatus) {
        Integer expectedStatus = orders.getStatus();
        orderStateMachine.transition(orders, targetStatus);

        int affectedRows = orderMapper.updateStatusByIdAndStatus(
                orders.getId(),
                targetStatus.getCode(),
                expectedStatus);
        if (affectedRows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2); // 1表示未单提醒 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号: " + ordersDB.getNumber());

        // 通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
