package com.sky.dto;

import com.sky.entity.ProductSku;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDTO implements Serializable {

    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private Integer sort;

    // 1支持溯源 0不支持
    private Integer traceEnabled;

    private String image;

    private String description;

    private Integer status;

    // 阶段性把 SKU 直接挂在商品 DTO 上，便于管理端一次性保存
    private List<ProductSku> skus = new ArrayList<>();
}
