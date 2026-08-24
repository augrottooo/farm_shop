package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.enumeration.OperationType;
import com.sky.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @AutoFill(OperationType.INSERT)
    void insert(Product product);

    @AutoFill(OperationType.UPDATE)
    void update(Product product);

    Page<ProductVO> pageQuery(ProductPageQueryDTO productPageQueryDTO);

    Product getById(Long id);

    @Select("select count(id) from product where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    List<Product> list(Product product);
}
