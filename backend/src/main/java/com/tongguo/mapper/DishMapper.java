package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    @Update("UPDATE dish SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Integer id, @Param("qty") Integer qty);

    @Update("UPDATE dish SET stock = stock + #{qty} WHERE id = #{id}")
    int addStock(@Param("id") Integer id, @Param("qty") Integer qty);
}
