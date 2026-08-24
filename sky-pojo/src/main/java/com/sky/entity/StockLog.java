package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long skuId;

    // 1预扣 2扣减 3回补
    private Integer businessType;

    private Long bizId;

    private Integer changeCount;

    private Integer beforeStock;

    private Integer afterStock;

    private String remark;

    private LocalDateTime createTime;
}
