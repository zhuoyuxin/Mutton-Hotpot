package com.tongguo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tongguo.mapper")
public class TongguoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TongguoApplication.class, args);
    }
}
