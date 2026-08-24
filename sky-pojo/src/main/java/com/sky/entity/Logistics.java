package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Logistics implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String carrierCode;

    private String carrierName;

    private String trackingNo;

    // 0待发货 1已发货 2运输中 3已签收 4异常
    private Integer shipStatus;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
