package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品规格表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    // SKU 编码
    private String skuCode;

    // SKU 名称
    private String skuName;

    // 规格信息，阶段性使用 JSON 字符串保存
    private String specInfo;

    private BigDecimal price;

    private Integer stock;

    private Integer lockedStock;

    // 0 下架 1 上架
    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
