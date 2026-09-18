package com.sky.mapper;

import com.sky.entity.UserCoupon;
import com.sky.vo.UserCouponVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserCouponMapper {

    void insert(UserCoupon userCoupon);

    void update(UserCoupon userCoupon);

    @Select("select * from user_coupon where id = #{id}")
    UserCoupon getById(Long id);

    @Select("select * from user_coupon where user_id = #{userId} order by id desc")
    List<UserCoupon> listByUserId(Long userId);

    List<UserCouponVO> listByUserIdAndStatus(@Param("userId") Long userId,
                                             @Param("couponStatus") Integer couponStatus);

    @Update("update user_coupon set coupon_status = 2, order_id = #{orderId}, update_time = NOW() " +
            "where id = #{couponId} and user_id = #{userId} and coupon_status = 1 " +
            "and (expire_time is null or expire_time >= NOW())")
    int lockById(@Param("couponId") Long couponId,
                 @Param("userId") Long userId,
                 @Param("orderId") Long orderId);

    @Update("update user_coupon set coupon_status = 3, used_time = NOW(), update_time = NOW() " +
            "where order_id = #{orderId} and coupon_status = 2")
    int useByOrderId(@Param("orderId") Long orderId);

    @Update("update user_coupon set coupon_status = 1, order_id = null, update_time = NOW() " +
            "where order_id = #{orderId} and coupon_status = 2")
    int unlockByOrderId(@Param("orderId") Long orderId);

    @Delete("delete from user_coupon where id = #{id}")
    void deleteById(Long id);
}
