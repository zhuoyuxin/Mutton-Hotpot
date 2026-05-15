package com.tongguo.dto;

import com.tongguo.entity.DiningSession;
import com.tongguo.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class SessionDetailDTO {
    private DiningSession session;
    private List<Orders> orders;
    private List<DishSummaryDTO> dishSummary;
    private int totalAmount;
}
