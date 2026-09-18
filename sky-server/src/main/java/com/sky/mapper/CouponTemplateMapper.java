package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CouponTemplatePageQueryDTO;
import com.sky.entity.CouponTemplate;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponTemplateMapper {

    @AutoFill(OperationType.INSERT)
    void insert(CouponTemplate couponTemplate);

    @AutoFill(OperationType.UPDATE)
    void update(CouponTemplate couponTemplate);

    Page<CouponTemplate> pageQuery(CouponTemplatePageQueryDTO queryDTO);

    @Select("select * from coupon_template where id = #{id}")
    CouponTemplate getById(Long id);

    @Update("update coupon_template set issue_count = issue_count + 1 " +
            "where id = #{id} and issue_count < total_count")
    int increaseIssueCountIfAvailable(@Param("id") Long id);

    List<CouponTemplate> list(CouponTemplate couponTemplate);

    @Delete("delete from coupon_template where id = #{id}")
    void deleteById(Long id);
}
