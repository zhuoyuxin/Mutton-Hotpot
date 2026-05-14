package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dining_session")
public class DiningSession {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("table_id")
    private Integer tableId;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("end_time")
    private LocalDateTime endTime;
}
