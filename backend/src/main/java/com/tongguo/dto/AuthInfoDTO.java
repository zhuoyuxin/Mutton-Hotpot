package com.tongguo.dto;

import lombok.Data;

@Data
public class AuthInfoDTO {
    private Integer id;
    private String username;
    private Integer mustChangePassword;
}
