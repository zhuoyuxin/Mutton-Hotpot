package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.dto.HourlyDTO;
import com.tongguo.dto.RevenueTrendDTO;
import com.tongguo.dto.TopDishDTO;
import com.tongguo.entity.OrderItem;
import com.tongguo.entity.Orders;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.mapper.OrderItemMapper;
import com.tongguo.mapper.OrdersMapper;
import com.tongguo.mapper.SessionCheckoutMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public List<RevenueTrendDTO> getRevenueTrend(String startDate, String endDate) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, start.atStartOfDay())
                        .le(SessionCheckout::getCheckoutTime, end.atTime(LocalTime.MAX))
        );

        Map<LocalDate, Long> revenueByDate = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            revenueByDate.put(date, 0L);
        }

        for (SessionCheckout checkout : checkouts) {
            if (checkout.getCheckoutTime() == null) {
                continue;
            }
            LocalDate date = checkout.getCheckoutTime().toLocalDate();
            long paid = checkout.getActualPaid() != null ? checkout.getActualPaid() : 0;
            revenueByDate.merge(date, paid, Long::sum);
        }

        List<RevenueTrendDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : revenueByDate.entrySet()) {
            RevenueTrendDTO dto = new RevenueTrendDTO();
            dto.setDate(entry.getKey().toString());
            dto.setRevenue(entry.getValue());
            result.add(dto);
        }
        return result;
    }

    public List<TopDishDTO> getTopDishes(String startDate, String endDate, Integer limit) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, start.atStartOfDay())
                        .le(SessionCheckout::getCheckoutTime, end.atTime(LocalTime.MAX))
        );
        if (checkouts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> sessionIds = checkouts.stream()
                .map(SessionCheckout::getSessionId)
                .collect(Collectors.toList());

        List<Orders> settledOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getSessionId, sessionIds)
                        .eq(Orders::getStatus, 4)
        );
        if (settledOrders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> orderIds = settledOrders.stream()
                .map(Orders::getId)
                .collect(Collectors.toList());
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .in(OrderItem::getStatus, 1, 2)
        );

        Map<String, long[]> dishAgg = new LinkedHashMap<>();
        for (OrderItem item : items) {
            long qty = item.getQuantity() != null ? item.getQuantity() : 0;
            long price = item.getDishPrice() != null ? item.getDishPrice() : 0;
            long[] acc = dishAgg.computeIfAbsent(item.getDishName(), key -> new long[]{0, 0});
            acc[0] += qty;
            acc[1] += price * qty;
        }

        List<TopDishDTO> dishList = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : dishAgg.entrySet()) {
            TopDishDTO dto = new TopDishDTO();
            dto.setDishName(entry.getKey());
            dto.setQuantity(entry.getValue()[0]);
            dto.setRevenue(entry.getValue()[1]);
            dishList.add(dto);
        }

        dishList.sort((left, right) -> Long.compare(right.getRevenue(), left.getRevenue()));
        int effectiveLimit = limit != null ? limit : 10;
        return dishList.stream().limit(effectiveLimit).collect(Collectors.toList());
    }

    public List<HourlyDTO> getHourlyDistribution(String startDate, String endDate) {
        LocalDate start = resolveStart(startDate);
        LocalDate end = resolveEnd(endDate);

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, start.atStartOfDay())
                        .le(Orders::getCreateTime, end.atTime(LocalTime.MAX))
                        .ne(Orders::getStatus, 5)
        );

        Map<Integer, Integer> hourMap = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            hourMap.put(hour, 0);
        }

        for (Orders order : orders) {
            if (order.getCreateTime() == null) {
                continue;
            }
            int hour = order.getCreateTime().getHour();
            hourMap.merge(hour, 1, Integer::sum);
        }

        List<HourlyDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : hourMap.entrySet()) {
            HourlyDTO dto = new HourlyDTO();
            dto.setHour(entry.getKey());
            dto.setCount(entry.getValue());
            result.add(dto);
        }
        return result;
    }

    private LocalDate resolveStart(String startDate) {
        return startDate != null && !startDate.isEmpty()
                ? parseDateOrThrow(startDate, "Invalid start date")
                : LocalDate.now().minusDays(6);
    }

    private LocalDate resolveEnd(String endDate) {
        return endDate != null && !endDate.isEmpty()
                ? parseDateOrThrow(endDate, "Invalid end date")
                : LocalDate.now();
    }

    private LocalDate parseDateOrThrow(String dateStr, String errorMsg) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
