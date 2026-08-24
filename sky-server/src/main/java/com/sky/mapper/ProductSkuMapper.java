package com.sky.mapper;

import com.sky.entity.ProductSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductSkuMapper {

    void insertBatch(List<ProductSku> productSkuList);

    @Select("select * from product_sku where id = #{id}")
    ProductSku getById(Long id);

    @Select("select * from product_sku where product_id = #{productId} order by sort asc, id asc")
    List<ProductSku> listByProductId(Long productId);

    @Delete("delete from product_sku where product_id = #{productId}")
    void deleteByProductId(Long productId);

    @Update("update product_sku set status = #{status} where product_id = #{productId}")
    void updateStatusByProductId(@Param("productId") Long productId, @Param("status") Integer status);

    void update(ProductSku productSku);
}
