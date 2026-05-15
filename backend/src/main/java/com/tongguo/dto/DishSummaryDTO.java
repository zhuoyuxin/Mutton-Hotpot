package com.tongguo.dto;

import lombok.Data;

@Data
public class DishSummaryDTO {
    private Integer dishId;
    private String dishName;
    private Integer dishPrice;
    private int quantity;
    private int amount;
}
