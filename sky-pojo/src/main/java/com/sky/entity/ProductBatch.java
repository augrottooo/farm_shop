package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品溯源批次表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private Long skuId;

    // 批次号
    private String batchNo;

    // 产地
    private String originPlace;

    private Long farmerId;

    private LocalDateTime harvestTime;

    private String qualityReportUrl;

    // 溯源码
    private String traceCode;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
