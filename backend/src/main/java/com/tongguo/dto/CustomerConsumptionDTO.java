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
    private Integer dishAmount;
    private Integer actualPaid;
    private Integer discountAmount;
    private Integer selfServiceCount;
    private Integer selfServiceUnitPrice;
    private Integer selfServiceAmount;
    private Integer tablewareCount;
    private Integer tablewareUnitPrice;
    private Integer tablewareAmount;
    private Integer pointsEarned;
    private Integer orderCount;
    private LocalDateTime checkoutTime;
    private List<DishSummaryDTO> dishSummary;
    private List<Orders> orders;
}
