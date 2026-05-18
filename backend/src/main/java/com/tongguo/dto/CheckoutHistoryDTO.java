package com.tongguo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckoutHistoryDTO {
    private Integer id;
    private Integer sessionId;
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
    private LocalDateTime checkoutTime;
    private String tableName;
    private String tableArea;
}
