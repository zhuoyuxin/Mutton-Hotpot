package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Update("UPDATE order_item SET status = #{toStatus} WHERE id = #{id} AND status = #{fromStatus}")
    int updateStatusIfCurrent(@Param("id") Integer id,
                              @Param("fromStatus") Integer fromStatus,
                              @Param("toStatus") Integer toStatus);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM order_item WHERE dish_id = #{dishId} AND status = 0")
    Integer sumPendingQuantityByDishId(@Param("dishId") Integer dishId);
}
