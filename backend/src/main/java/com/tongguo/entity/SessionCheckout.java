package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("session_checkout")
public class SessionCheckout {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("session_id")
    private Integer sessionId;
    @TableField("total_amount")
    private Integer totalAmount;
    @TableField("actual_paid")
    private Integer actualPaid;
    @TableField("discount_amount")
    private Integer discountAmount;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("points_earned")
    private Integer pointsEarned;
    @TableField("checkout_time")
    private LocalDateTime checkoutTime;
}
