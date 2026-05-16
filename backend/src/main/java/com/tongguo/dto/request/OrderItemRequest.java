package com.tongguo.dto.request;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Integer dishId;
    private Integer quantity;
}
