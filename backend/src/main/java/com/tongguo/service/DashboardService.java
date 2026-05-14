package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private DiningSessionMapper sessionMapper;

    public Map<String, Object> getTodayData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> data = new HashMap<>();

        Long orderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, todayStart)
                        .ne(Orders::getStatus, 5)
        );
        data.put("todayOrders", orderCount);

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, todayStart)
        );
        int revenue = checkouts.stream().mapToInt(SessionCheckout::getActualPaid).sum();
        data.put("todayRevenue", revenue);

        List<TableInfo> tables = tableInfoMapper.selectList(null);
        long freeTables = tables.stream().filter(t -> t.getStatus() == 0).count();
        long busyTables = tables.stream().filter(t -> t.getStatus() == 1).count();
        data.put("totalTables", tables.size());
        data.put("freeTables", freeTables);
        data.put("busyTables", busyTables);

        Long activeSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>().eq(DiningSession::getStatus, 0)
        );
        data.put("activeSessions", activeSessions);

        return data;
    }
}
