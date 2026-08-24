package com.sky.service;

import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.result.PageResult;
import com.sky.vo.ProductVO;

import java.util.List;

public interface ProductService {

    void save(ProductDTO productDTO);

    PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO);

    ProductVO getById(Long id);

    void update(ProductDTO productDTO);

    void startOrStop(Integer status, Long id);

    List<Product> listByCategory(Long categoryId);

    List<com.sky.entity.ProductSku> listSkuByProductId(Long productId);
}
