package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("checkout_id")
    private Integer checkoutId;
    private Integer points;
    private Integer type;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
}
