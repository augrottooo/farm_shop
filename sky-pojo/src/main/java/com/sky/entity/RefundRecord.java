package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String refundNo;

    private BigDecimal refundAmount;

    // 0待处理 1成功 2失败
    private Integer refundStatus;

    private String reason;

    private String channel;

    private String transactionId;

    private LocalDateTime appliedAt;

    private LocalDateTime successAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
