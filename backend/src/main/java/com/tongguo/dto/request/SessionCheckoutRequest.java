package com.tongguo.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SessionCheckoutRequest {
    private BigDecimal actualPaid;
    private String phone;
}
