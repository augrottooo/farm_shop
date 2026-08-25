package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 用于替换微信支付更新数据库状态的问题
     * @param orderStatus
     * @param orderPaidStatus
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus},checkout_time = #{checkoutTime} " +
            "where id = #{id} and status = #{expectedStatus}")
    int updateStatus(@Param("orderStatus") Integer orderStatus,
                     @Param("orderPaidStatus") Integer orderPaidStatus,
                     @Param("checkoutTime") LocalDateTime checkoutTime,
                     @Param("id") Long id,
                     @Param("expectedStatus") Integer expectedStatus);

    /**
     * 带原状态条件的订单状态迁移，防止并发重复操作。
     */
    @Update("update orders set status = #{targetStatus} where id = #{id} and status = #{expectedStatus}")
    int updateStatusByIdAndStatus(@Param("id") Long id,
                                  @Param("targetStatus") Integer targetStatus,
                                  @Param("expectedStatus") Integer expectedStatus);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time <#{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);
}
