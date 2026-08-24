package com.sky.mapper;

import com.sky.entity.RefundRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RefundRecordMapper {

    void insert(RefundRecord refundRecord);

    void update(RefundRecord refundRecord);

    @Select("select * from refund_record where id = #{id}")
    RefundRecord getById(Long id);

    @Select("select * from refund_record where order_id = #{orderId} order by id desc")
    List<RefundRecord> listByOrderId(Long orderId);

    @Delete("delete from refund_record where id = #{id}")
    void deleteById(Long id);
}
