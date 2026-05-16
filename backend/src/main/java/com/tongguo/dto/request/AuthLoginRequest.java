package com.tongguo.dto.request;

import lombok.Data;

@Data
public class AuthLoginRequest {
    private String username;
    private String password;
}
