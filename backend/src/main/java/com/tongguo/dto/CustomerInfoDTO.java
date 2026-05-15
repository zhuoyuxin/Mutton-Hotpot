package com.tongguo.dto;

import lombok.Data;

@Data
public class CustomerInfoDTO {
    private Integer id;
    private String phone;
    private String name;
    private Integer points;
    private Integer totalSpent;
}
