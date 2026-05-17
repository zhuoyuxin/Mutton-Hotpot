package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.SessionCheckout;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionCheckoutMapper extends BaseMapper<SessionCheckout> {

    @Update("UPDATE session_checkout SET customer_id = #{targetCustomerId} " +
            "WHERE customer_id = #{sourceCustomerId}")
    int moveCustomerCheckouts(@Param("sourceCustomerId") Integer sourceCustomerId,
                              @Param("targetCustomerId") Integer targetCustomerId);
}
