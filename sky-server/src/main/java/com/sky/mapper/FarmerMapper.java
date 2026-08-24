package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.Farmer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FarmerMapper {

    void insert(Farmer farmer);

    void update(Farmer farmer);

    Page<Farmer> pageQuery(Farmer farmer);

    @Select("select * from farmer where id = #{id}")
    Farmer getById(Long id);

    List<Farmer> list(Farmer farmer);

    @Delete("delete from farmer where id = #{id}")
    void deleteById(Long id);
}
