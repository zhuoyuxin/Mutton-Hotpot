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
    @TableField("dish_amount")
    private Integer dishAmount;
    @TableField("actual_paid")
    private Integer actualPaid;
    @TableField("discount_amount")
    private Integer discountAmount;
    @TableField("self_service_count")
    private Integer selfServiceCount;
    @TableField("self_service_unit_price")
    private Integer selfServiceUnitPrice;
    @TableField("self_service_amount")
    private Integer selfServiceAmount;
    @TableField("tableware_count")
    private Integer tablewareCount;
    @TableField("tableware_unit_price")
    private Integer tablewareUnitPrice;
    @TableField("tableware_amount")
    private Integer tablewareAmount;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("points_earned")
    private Integer pointsEarned;
    @TableField("checkout_time")
    private LocalDateTime checkoutTime;
}
