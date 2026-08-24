package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品主表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 分类id
    private Long categoryId;

    // 商品名称
    private String name;

    // 商品副标题
    private String subtitle;

    // 排序
    private Integer sort;

    // 主图
    private String image;

    // 商品描述
    private String description;

    // 状态 0 下架 1 上架
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
