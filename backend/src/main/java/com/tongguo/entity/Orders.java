package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("order_no")
    private String orderNo;
    @TableField("session_id")
    private Integer sessionId;
    @TableField("table_id")
    private Integer tableId;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("total_amount")
    private Integer totalAmount;
    private Integer status;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private List<OrderItem> items;
}
