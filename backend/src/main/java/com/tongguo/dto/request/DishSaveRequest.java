package com.tongguo.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishSaveRequest {
    private Integer id;
    private Integer categoryId;
    private String name;
    private BigDecimal price;
    private Boolean allowHalfPortion;
    private BigDecimal halfPrice;
    private String image;
    private String description;
    private Integer stock;
    private Integer sortOrder;
}
