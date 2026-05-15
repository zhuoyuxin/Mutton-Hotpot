package com.tongguo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckoutHistoryDTO {
    private Integer id;
    private Integer sessionId;
    private Integer totalAmount;
    private Integer actualPaid;
    private Integer discountAmount;
    private Integer pointsEarned;
    private LocalDateTime checkoutTime;
    private String tableName;
    private String tableArea;
}
