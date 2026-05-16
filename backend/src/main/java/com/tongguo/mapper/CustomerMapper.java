package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Update("UPDATE customer " +
            "SET points = points + #{pointsDelta}, " +
            "    total_spent = total_spent + #{spentDelta}, " +
            "    update_time = datetime('now','localtime') " +
            "WHERE id = #{id} AND points + #{pointsDelta} >= 0")
    int adjustBalances(@Param("id") Integer id,
                       @Param("pointsDelta") Integer pointsDelta,
                       @Param("spentDelta") Integer spentDelta);
}
