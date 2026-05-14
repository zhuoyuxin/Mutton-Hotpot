package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("table_info")
public class TableInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String area;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
}
