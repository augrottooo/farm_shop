package com.sky.mapper;

import com.sky.entity.Logistics;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LogisticsMapper {

    void insert(Logistics logistics);

    void update(Logistics logistics);

    @Select("select * from logistics where id = #{id}")
    Logistics getById(Long id);

    @Select("select * from logistics where order_id = #{orderId} order by id desc")
    List<Logistics> listByOrderId(Long orderId);

    @Delete("delete from logistics where id = #{id}")
    void deleteById(Long id);
}
