package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.dto.*;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    @Autowired
    private DiningSessionMapper sessionMapper;

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Transactional
    public DiningSession getOrCreateSession(Integer tableId) {
        DiningSession session = findActiveSessionByTableId(tableId);
        if (session != null) {
            syncTableStatus(tableId, 1);
            return session;
        }

        session = new DiningSession();
        session.setTableId(tableId);
        session.setStatus(0);
        try {
            sessionMapper.insert(session);
        } catch (DataIntegrityViolationException e) {
            session = findActiveSessionByTableId(tableId);
            if (session == null) {
                throw e;
            }
        }
        if (session.getId() == null) {
            session = findActiveSessionByTableId(tableId);
        }
        syncTableStatus(tableId, 1);
        return session;
    }

    public DiningSession createWalkInSession() {
        DiningSession session = new DiningSession();
        session.setStatus(0);
        sessionMapper.insert(session);
        return session;
    }

    public DiningSession getCurrentByTableId(Integer tableId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        );
    }

    public SessionDetailDTO getDetail(Integer sessionId) {
        DiningSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, sessionId)
                        .ne(Orders::getStatus, 5)
        );

        List<Integer> orderIds = orders.stream()
                .map(Orders::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<OrderItem> allItems = orderIds.isEmpty() ? Collections.emptyList() :
                orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId, orderIds)
                );
        Map<Integer, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()));
        }

        List<OrderItem> payableItems = allItems.stream()
                .filter(item -> item.getStatus() != null && (item.getStatus() == 1 || item.getStatus() == 2))
                .collect(Collectors.toList());

        int totalAmount = 0;
        for (OrderItem item : payableItems) {
            totalAmount += item.getDishPrice() * item.getQuantity();
        }

        SessionDetailDTO dto = new SessionDetailDTO();
        dto.setSession(session);
        dto.setOrders(orders);
        dto.setDishSummary(buildDishSummary(payableItems));
        dto.setTotalAmount(totalAmount);
        return dto;
    }

    @Transactional
    public SessionCheckout checkout(Integer sessionId, Integer actualPaidFen, String phone) {
        SessionCheckout existing = checkoutMapper.selectOne(
                new LambdaQueryWrapper<SessionCheckout>().eq(SessionCheckout::getSessionId, sessionId)
        );
        if (existing != null) {
            return existing;
        }

        DiningSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() != 0) {
            throw new IllegalArgumentException("会话不存在或已结束");
        }

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, sessionId)
                        .notIn(Orders::getStatus, 4, 5)
        );

        for (Orders order : orders) {
            if (order.getStatus() == 0) {
                throw new IllegalArgumentException("存在待确认订单，请先确认或取消后再结账");
            }
        }

        List<Integer> orderIds = new ArrayList<>();
        for (Orders order : orders) { orderIds.add(order.getId()); }
        List<OrderItem> allItems = orderIds.isEmpty() ? Collections.emptyList() :
                orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId, orderIds)
                                .in(OrderItem::getStatus, 1, 2)
                );

        int totalAmount = 0;
        for (OrderItem item : allItems) {
            totalAmount += item.getDishPrice() * item.getQuantity();
        }

        if (actualPaidFen == null) {
            throw new IllegalArgumentException("实收金额不能为空");
        }
        if (actualPaidFen < 0) {
            throw new IllegalArgumentException("实收金额不能小于0");
        }
        if (actualPaidFen > totalAmount) {
            throw new IllegalArgumentException("实收金额不能大于应结金额");
        }

        int discountAmount = totalAmount - actualPaidFen;
        int pointsEarned = actualPaidFen / 100;

        SessionCheckout checkout = new SessionCheckout();
        checkout.setSessionId(sessionId);
        checkout.setTotalAmount(totalAmount);
        checkout.setActualPaid(actualPaidFen);
        checkout.setDiscountAmount(discountAmount);
        checkout.setPointsEarned(pointsEarned);
        checkout.setCheckoutTime(LocalDateTime.now());

        Integer customerId = null;
        if (phone != null && !phone.trim().isEmpty()) {
            Customer customer = customerService.findOrCreateByPhone(phone);
            customerId = customer.getId();
            checkout.setCustomerId(customerId);
        }

        try {
            checkoutMapper.insert(checkout);
        } catch (DataIntegrityViolationException e) {
            SessionCheckout duplicated = checkoutMapper.selectOne(
                    new LambdaQueryWrapper<SessionCheckout>().eq(SessionCheckout::getSessionId, sessionId)
            );
            if (duplicated != null) {
                return duplicated;
            }
            throw e;
        }

        if (customerId != null) {
            PointsRecord pointsRecord = new PointsRecord();
            pointsRecord.setCustomerId(customerId);
            pointsRecord.setCheckoutId(checkout.getId());
            pointsRecord.setPoints(pointsEarned);
            pointsRecord.setType(0);
            pointsRecord.setRemark("消费获得");
            pointsRecordMapper.insert(pointsRecord);

            Customer customer = customerMapper.selectById(customerId);
            customer.setPoints(customer.getPoints() + pointsEarned);
            customer.setTotalSpent(customer.getTotalSpent() + actualPaidFen);
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(customer);
        }

        for (Orders order : orders) {
            order.setStatus(4);
            order.setUpdateTime(LocalDateTime.now());
            ordersMapper.updateById(order);
        }

        session.setStatus(1);
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        syncTableStatus(session.getTableId(), 0);

        return checkout;
    }

    private DiningSession findActiveSessionByTableId(Integer tableId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        );
    }

    private void syncTableStatus(Integer tableId, int expectedStatus) {
        if (tableId == null) {
            return;
        }
        TableInfo table = tableInfoMapper.selectById(tableId);
        if (table == null || Objects.equals(table.getStatus(), expectedStatus)) {
            return;
        }
        table.setStatus(expectedStatus);
        tableInfoMapper.updateById(table);
    }

    public List<CheckoutHistoryDTO> getCheckoutHistory(String startDate, String endDate) {
        LambdaQueryWrapper<SessionCheckout> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(SessionCheckout::getCheckoutTime, parseDateOrThrow(startDate, "开始日期格式不正确").atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(SessionCheckout::getCheckoutTime, parseDateOrThrow(endDate, "结束日期格式不正确").atTime(23, 59, 59));
        }
        wrapper.orderByDesc(SessionCheckout::getCheckoutTime);
        List<SessionCheckout> checkouts = checkoutMapper.selectList(wrapper);

        Set<Integer> sessionIds = checkouts.stream()
                .map(SessionCheckout::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, DiningSession> sessionMap = new HashMap<>();
        Map<Integer, TableInfo> tableMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<DiningSession> sessions = sessionMapper.selectList(
                    new LambdaQueryWrapper<DiningSession>().in(DiningSession::getId, sessionIds)
            );
            sessionMap = sessions.stream().collect(Collectors.toMap(DiningSession::getId, s -> s));

            Set<Integer> tableIds = sessions.stream()
                    .map(DiningSession::getTableId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!tableIds.isEmpty()) {
                tableMap = tableInfoMapper.selectList(
                        new LambdaQueryWrapper<TableInfo>().in(TableInfo::getId, tableIds)
                ).stream().collect(Collectors.toMap(TableInfo::getId, t -> t));
            }
        }

        List<CheckoutHistoryDTO> result = new ArrayList<>();
        for (SessionCheckout c : checkouts) {
            CheckoutHistoryDTO dto = new CheckoutHistoryDTO();
            dto.setId(c.getId());
            dto.setSessionId(c.getSessionId());
            dto.setTotalAmount(c.getTotalAmount());
            dto.setActualPaid(c.getActualPaid());
            dto.setDiscountAmount(c.getDiscountAmount());
            dto.setPointsEarned(c.getPointsEarned());
            dto.setCheckoutTime(c.getCheckoutTime());

            DiningSession session = sessionMap.get(c.getSessionId());
            if (session != null && session.getTableId() != null) {
                TableInfo table = tableMap.get(session.getTableId());
                if (table != null) {
                    dto.setTableName(table.getName());
                    dto.setTableArea(table.getArea());
                }
            }
            result.add(dto);
        }
        return result;
    }

    private LocalDate parseDateOrThrow(String dateStr, String errorMsg) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private List<DishSummaryDTO> buildDishSummary(List<OrderItem> items) {
        Map<Integer, DishSummaryDTO> summaryByDish = new LinkedHashMap<>();
        for (OrderItem item : items) {
            DishSummaryDTO summary = summaryByDish.computeIfAbsent(item.getDishId(), key -> {
                DishSummaryDTO dto = new DishSummaryDTO();
                dto.setDishId(item.getDishId());
                dto.setDishName(item.getDishName());
                dto.setDishPrice(item.getDishPrice());
                dto.setQuantity(0);
                dto.setAmount(0);
                return dto;
            });
            summary.setQuantity(summary.getQuantity() + item.getQuantity());
            summary.setAmount(summary.getAmount() + item.getDishPrice() * item.getQuantity());
        }
        return new ArrayList<>(summaryByDish.values());
    }
}
