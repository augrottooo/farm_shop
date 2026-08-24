package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * SKU 绝对库存设置参数
 */
@Data
public class SkuStockUpdateDTO implements Serializable {

    private Long skuId;

    // 目标库存，不是增量
    private Integer stock;
}
