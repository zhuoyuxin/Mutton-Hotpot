package com.tongguo.dto;

import lombok.Data;

@Data
public class CustomerAuthDTO {
    private String token;
    private Long expiresAt;
    private String loginType;
    private CustomerInfoDTO customer;
}
