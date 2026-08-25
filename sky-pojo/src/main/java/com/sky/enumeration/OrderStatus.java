package com.sky.enumeration;

import lombok.Getter;

/**
 * 订单状态
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(1, "待付款"),
    WAITING_SHIP(2, "待发货"),
    TRANSPORTING(3, "运输中"),
    SIGNED(4, "已签收"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
