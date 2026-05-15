package com.tongguo.dto;

import com.tongguo.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class TableOverviewDTO {
    private Integer id;
    private String name;
    private String area;
    private Integer status;
    private List<Orders> orders;
    private int totalItems;
    private long servedItems;
    private long pendingItems;
}
