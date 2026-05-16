package com.tongguo.dto;

import com.tongguo.entity.Orders;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerConsumptionDTO {
    private Integer checkoutId;
    private Integer sessionId;
    private String tableName;
    private String tableArea;
    private Integer totalAmount;
    private Integer actualPaid;
    private Integer discountAmount;
    private Integer pointsEarned;
    private Integer orderCount;
    private LocalDateTime checkoutTime;
    private List<DishSummaryDTO> dishSummary;
    private List<Orders> orders;
}
