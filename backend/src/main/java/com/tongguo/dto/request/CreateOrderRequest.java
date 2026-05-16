package com.tongguo.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    private Integer tableId;
    private Integer sessionId;
    private List<OrderItemRequest> items;
    private String phone;
    private String remark;
}
