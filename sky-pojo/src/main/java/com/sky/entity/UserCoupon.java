package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领券记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Integer UNUSED = 1;
    public static final Integer LOCKED = 2;
    public static final Integer USED = 3;
    public static final Integer EXPIRED = 4;

    private Long id;

    private Long userId;

    private Long couponTemplateId;

    private String couponCode;

    // 1未使用 2已锁定 3已使用 4已过期
    private Integer couponStatus;

    private Long orderId;

    private LocalDateTime receiveTime;

    private LocalDateTime usedTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
