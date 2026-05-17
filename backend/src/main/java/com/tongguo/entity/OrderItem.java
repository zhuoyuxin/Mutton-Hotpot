package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("order_id")
    private Integer orderId;
    @TableField("dish_id")
    private Integer dishId;
    @TableField("dish_name")
    private String dishName;
    @TableField("dish_price")
    private Integer dishPrice;
    @TableField("portion_type")
    private String portionType;
    private Integer quantity;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
}
