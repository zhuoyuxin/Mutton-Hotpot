package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public List<Map<String, Object>> getRevenueTrend(String startDate, String endDate) {
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : LocalDate.now().minusDays(6);
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : LocalDate.now();

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, start.atStartOfDay())
                        .le(SessionCheckout::getCheckoutTime, end.atTime(LocalTime.MAX))
        );

        Map<LocalDate, Integer> revenueByDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            revenueByDate.put(d, 0);
        }
        for (SessionCheckout c : checkouts) {
            LocalDate date = c.getCheckoutTime().toLocalDate();
            revenueByDate.merge(date, c.getActualPaid(), Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : revenueByDate.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey().toString());
            row.put("revenue", entry.getValue());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getTopDishes(String startDate, String endDate, Integer limit) {
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : LocalDate.now().minusDays(6);
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : LocalDate.now();

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, start.atStartOfDay())
                        .le(SessionCheckout::getCheckoutTime, end.atTime(LocalTime.MAX))
        );
        if (checkouts.isEmpty()) return Collections.emptyList();

        List<Integer> sessionIds = checkouts.stream().map(SessionCheckout::getSessionId).collect(Collectors.toList());

        List<Orders> settledOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getSessionId, sessionIds)
                        .eq(Orders::getStatus, 4)
        );
        if (settledOrders.isEmpty()) return Collections.emptyList();

        List<Integer> orderIds = settledOrders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .in(OrderItem::getStatus, 1, 2)
        );

        Map<String, Map<String, Object>> dishMap = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Map<String, Object> data = dishMap.computeIfAbsent(item.getDishName(), k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("dishName", k);
                m.put("quantity", 0);
                m.put("revenue", 0);
                return m;
            });
            data.put("quantity", (int) data.get("quantity") + item.getQuantity());
            data.put("revenue", (int) data.get("revenue") + item.getDishPrice() * item.getQuantity());
        }

        return dishMap.values().stream()
                .sorted((a, b) -> (int) b.get("revenue") - (int) a.get("revenue"))
                .limit(limit != null ? limit : 10)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getHourlyDistribution(String startDate, String endDate) {
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : LocalDate.now().minusDays(6);
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : LocalDate.now();

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, start.atStartOfDay())
                        .le(Orders::getCreateTime, end.atTime(LocalTime.MAX))
                        .ne(Orders::getStatus, 5)
        );

        Map<Integer, Integer> hourMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourMap.put(h, 0);
        }
        for (Orders order : orders) {
            int hour = order.getCreateTime().getHour();
            hourMap.merge(hour, 1, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : hourMap.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("hour", entry.getKey());
            row.put("count", entry.getValue());
            result.add(row);
        }
        return result;
    }
}
