package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String couponName;

    // 1满减 2折扣 3无门槛
    private Integer couponType;

    private BigDecimal thresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private Integer totalCount;

    // 已领取数，复用原 issue_count 字段
    private Integer issueCount;

    private Integer receiveLimit;

    private Integer validDays;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // 0 停用 1 启用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
