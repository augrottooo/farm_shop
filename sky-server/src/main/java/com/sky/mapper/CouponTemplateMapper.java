package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.CouponTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponTemplateMapper {

    void insert(CouponTemplate couponTemplate);

    void update(CouponTemplate couponTemplate);

    Page<CouponTemplate> pageQuery(CouponTemplate couponTemplate);

    @Select("select * from coupon_template where id = #{id}")
    CouponTemplate getById(Long id);

    List<CouponTemplate> list(CouponTemplate couponTemplate);

    @Delete("delete from coupon_template where id = #{id}")
    void deleteById(Long id);
}
