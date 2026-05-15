package com.tongguo.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private Long todayOrders;
    private int todayRevenue;
    private int totalTables;
    private long freeTables;
    private long busyTables;
    private Long activeSessions;
}
