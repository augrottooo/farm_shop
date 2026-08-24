package com.sky.mapper;

import com.sky.entity.ProductBatch;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductBatchMapper {

    void insert(ProductBatch productBatch);

    void update(ProductBatch productBatch);

    @Select("select * from product_batch where id = #{id}")
    ProductBatch getById(Long id);

    @Select("select * from product_batch where product_id = #{productId} order by id desc")
    List<ProductBatch> listByProductId(Long productId);

    @Delete("delete from product_batch where product_id = #{productId}")
    void deleteByProductId(Long productId);
}
