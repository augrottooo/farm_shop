package com.sky.mapper;

import com.sky.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockLogMapper {

    void insert(StockLog stockLog);

    @Select("select * from stock_log where sku_id = #{skuId} order by id desc")
    List<StockLog> listBySkuId(Long skuId);
}
