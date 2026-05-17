package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("category_id")
    private Integer categoryId;
    private String name;
    private Integer price;
    private String image;
    private String description;
    private Integer status;
    private Integer stock;
    @TableField("allow_half_portion")
    private Integer allowHalfPortion;
    @TableField("half_price")
    private Integer halfPrice;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
