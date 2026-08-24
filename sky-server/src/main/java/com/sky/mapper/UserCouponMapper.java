package com.sky.mapper;

import com.sky.entity.UserCoupon;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserCouponMapper {

    void insert(UserCoupon userCoupon);

    void update(UserCoupon userCoupon);

    @Select("select * from user_coupon where id = #{id}")
    UserCoupon getById(Long id);

    @Select("select * from user_coupon where user_id = #{userId} order by id desc")
    List<UserCoupon> listByUserId(Long userId);

    @Select("select * from user_coupon where user_id = #{userId} and coupon_status = #{couponStatus}")
    List<UserCoupon> listByUserIdAndStatus(@Param("userId") Long userId,
                                           @Param("couponStatus") Integer couponStatus);

    @Delete("delete from user_coupon where id = #{id}")
    void deleteById(Long id);
}
