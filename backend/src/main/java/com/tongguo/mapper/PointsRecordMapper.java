package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Update("UPDATE points_record SET customer_id = #{targetCustomerId} " +
            "WHERE customer_id = #{sourceCustomerId}")
    int moveCustomerPoints(@Param("sourceCustomerId") Integer sourceCustomerId,
                           @Param("targetCustomerId") Integer targetCustomerId);
}
