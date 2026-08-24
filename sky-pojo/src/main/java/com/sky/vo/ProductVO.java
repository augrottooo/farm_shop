package com.sky.vo;

import com.sky.entity.Product;
import com.sky.entity.ProductSku;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品详情返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO extends Product implements Serializable {

    private String categoryName;

    private List<ProductSku> skus = new ArrayList<>();
}
