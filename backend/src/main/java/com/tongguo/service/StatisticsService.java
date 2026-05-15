package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

    private LocalDate parseDateOrThrow(String dateStr, String errorMsg) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private LocalDate resolveStart(String startDate) {
        return startDate != null && !startDate.isEmpty()
                ? parseDateOrThrow(startDate, "开始日期格式不正确")
                : LocalDate.now().minusDays(6);
    }

    private LocalDate resolveEnd(String endDate) {
        return endDate != null && !endDate.isEmpty()
                ? parseDateOrThrow(endDate, "结束日期格式不正确")
                : LocalDate.now();
    }

    public List<Map<String, Object>> getRevenueTrend(String startDate, String endDate) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, start.atStartOfDay())
                        .le(SessionCheckout::getCheckoutTime, end.atTime(LocalTime.MAX))
        );

        Map<LocalDate, Long> revenueByDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            revenueByDate.put(d, 0L);
        }
        for (SessionCheckout c : checkouts) {
            if (c.getCheckoutTime() == null) continue;
            LocalDate date = c.getCheckoutTime().toLocalDate();
            long paid = c.getActualPaid() != null ? c.getActualPaid() : 0;
            revenueByDate.merge(date, paid, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : revenueByDate.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey().toString());
            row.put("revenue", entry.getValue());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getTopDishes(String startDate, String endDate, Integer limit) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

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

        Map<String, long[]> dishAgg = new LinkedHashMap<>();
        for (OrderItem item : items) {
            long qty = item.getQuantity() != null ? item.getQuantity() : 0;
            long price = item.getDishPrice() != null ? item.getDishPrice() : 0;
            long[] acc = dishAgg.computeIfAbsent(item.getDishName(), k -> new long[]{0, 0});
            acc[0] += qty;
            acc[1] += price * qty;
        }

        List<Map<String, Object>> dishList = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : dishAgg.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("dishName", entry.getKey());
            row.put("quantity", entry.getValue()[0]);
            row.put("revenue", entry.getValue()[1]);
            dishList.add(row);
        }

        dishList.sort((a, b) -> Long.compare((long) b.get("revenue"), (long) a.get("revenue")));
        int effectiveLimit = limit != null ? limit : 10;
        return dishList.stream().limit(effectiveLimit).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getHourlyDistribution(String startDate, String endDate) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

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
            if (order.getCreateTime() == null) continue;
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
