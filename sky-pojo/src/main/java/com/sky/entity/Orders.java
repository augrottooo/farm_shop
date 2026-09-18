package com.sky.entity;

import com.sky.enumeration.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 订单状态（兼容旧常量名）
     * 1待付款 2待发货 3运输中 4已签收 5已完成 6已取消
     */
    public static final Integer PENDING_PAYMENT = OrderStatus.PENDING_PAYMENT.getCode();
    public static final Integer TO_BE_CONFIRMED = OrderStatus.WAITING_SHIP.getCode();
    public static final Integer CONFIRMED = OrderStatus.TRANSPORTING.getCode();
    public static final Integer DELIVERY_IN_PROGRESS = OrderStatus.SIGNED.getCode();
    public static final Integer COMPLETED = OrderStatus.COMPLETED.getCode();
    public static final Integer CANCELLED = OrderStatus.CANCELLED.getCode();

    /**
     * 支付状态 0未支付 1已支付 2退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单号
    private String number;

    //订单状态 1待付款 2待发货 3运输中 4已签收 5已完成 6已取消
    private Integer status;

    //下单用户id
    private Long userId;

    //地址id
    private Long addressBookId;

    //使用的用户券ID
    private Long couponId;

    //下单时间
    private LocalDateTime orderTime;

    //结账时间
    private LocalDateTime checkoutTime;

    //支付方式 1微信，2支付宝
    private Integer payMethod;

    //支付状态 0未支付 1已支付 2退款
    private Integer payStatus;

    //实收金额
    private BigDecimal amount;

    //原始金额
    private BigDecimal originalAmount;

    //优惠金额
    private BigDecimal discountAmount;

    //备注
    private String remark;

    //用户名
    private String userName;

    //手机号
    private String phone;

    //地址
    private String address;

    //收货人
    private String consignee;

    //订单取消原因
    private String cancelReason;

    //订单拒绝原因
    private String rejectionReason;

    //订单取消时间
    private LocalDateTime cancelTime;

    //预计送达时间
    private LocalDateTime estimatedDeliveryTime;

    //配送状态  1立即送出  0选择具体时间
    private Integer deliveryStatus;

    //送达时间
    private LocalDateTime deliveryTime;

    //打包费
    private int packAmount;

    //餐具数量
    private int tablewareNumber;

    //餐具数量状态  1按餐量提供  0选择具体数量
    private Integer tablewareStatus;
}
