package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.dto.DashboardDTO;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    public DashboardDTO getTodayData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        DashboardDTO dto = new DashboardDTO();

        Long orderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, todayStart)
                        .ne(Orders::getStatus, 5)
        );
        dto.setTodayOrders(orderCount);

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, todayStart)
        );
        int revenue = checkouts.stream().mapToInt(SessionCheckout::getActualPaid).sum();
        dto.setTodayRevenue(revenue);

        List<TableInfo> tables = tableInfoMapper.selectList(null);
        long freeTables = tables.stream().filter(t -> t.getStatus() == 0).count();
        long busyTables = tables.stream().filter(t -> t.getStatus() == 1).count();
        dto.setTotalTables(tables.size());
        dto.setFreeTables(freeTables);
        dto.setBusyTables(busyTables);

        Long activeSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>().eq(DiningSession::getStatus, 0)
        );
        dto.setActiveSessions(activeSessions);

        return dto;
    }
}
