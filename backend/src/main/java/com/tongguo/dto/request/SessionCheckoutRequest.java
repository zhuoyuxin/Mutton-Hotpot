package com.tongguo.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SessionCheckoutRequest {
    private BigDecimal actualPaid;
    private Integer selfServiceCount;
    private BigDecimal selfServiceUnitPrice;
    private Integer tablewareCount;
    private BigDecimal tablewareUnitPrice;
    private String phone;
}
