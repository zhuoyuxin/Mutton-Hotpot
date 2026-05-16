package com.tongguo.dto.request;

import lombok.Data;

@Data
public class ServeItemRequest {
    private Integer quantity;
    private Integer expectedQuantity;
}
