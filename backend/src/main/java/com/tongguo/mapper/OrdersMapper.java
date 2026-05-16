package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Update("UPDATE orders SET status = #{toStatus}, update_time = datetime('now','localtime') " +
            "WHERE id = #{id} AND status = #{fromStatus}")
    int updateStatusIfCurrent(@Param("id") Integer id,
                              @Param("fromStatus") Integer fromStatus,
                              @Param("toStatus") Integer toStatus);

    @Update("UPDATE orders SET customer_id = #{customerId}, update_time = datetime('now','localtime') " +
            "WHERE session_id = #{sessionId}")
    int updateCustomerBySessionId(@Param("sessionId") Integer sessionId,
                                  @Param("customerId") Integer customerId);
}
