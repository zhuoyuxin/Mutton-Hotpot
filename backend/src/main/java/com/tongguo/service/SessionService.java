package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.constant.PortionType;
import com.tongguo.dto.CheckoutHistoryDTO;
import com.tongguo.dto.CustomerConsumptionDTO;
import com.tongguo.dto.DishSummaryDTO;
import com.tongguo.dto.SessionDetailDTO;
import com.tongguo.entity.Customer;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.OrderItem;
import com.tongguo.entity.Orders;
import com.tongguo.entity.PointsRecord;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.entity.TableInfo;
import com.tongguo.mapper.CustomerMapper;
import com.tongguo.mapper.DiningSessionMapper;
import com.tongguo.mapper.OrderItemMapper;
import com.tongguo.mapper.OrdersMapper;
import com.tongguo.mapper.PointsRecordMapper;
import com.tongguo.mapper.SessionCheckoutMapper;
import com.tongguo.mapper.TableInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionService {

    @Value("${checkout.default-self-service-unit-price-fen:0}")
    private Integer defaultSelfServiceUnitPriceFen;

    @Value("${checkout.default-tableware-unit-price-fen:0}")
    private Integer defaultTablewareUnitPriceFen;

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
        requireTable(tableId);

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
        if (session == null) {
            throw new IllegalArgumentException("Session does not exist");
        }

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
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
                );
        Map<Integer, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()));
        }

        List<OrderItem> payableItems = allItems.stream()
                .filter(item -> item.getStatus() != null && (item.getStatus() == 1 || item.getStatus() == 2))
                .collect(Collectors.toList());

        int dishAmount = 0;
        for (OrderItem item : payableItems) {
            dishAmount += item.getDishPrice() * item.getQuantity();
        }

        SessionDetailDTO dto = new SessionDetailDTO();
        dto.setSession(session);
        dto.setOrders(orders);
        dto.setDishSummary(buildDishSummary(payableItems));
        dto.setDishAmount(dishAmount);
        dto.setTotalAmount(dishAmount);
        dto.setDefaultSelfServiceUnitPrice(safeNonNegative(defaultSelfServiceUnitPriceFen));
        dto.setDefaultTablewareUnitPrice(safeNonNegative(defaultTablewareUnitPriceFen));
        return dto;
    }

    @Transactional
    public SessionCheckout checkout(Integer sessionId,
                                    Integer actualPaidFen,
                                    String phone,
                                    Integer selfServiceCount,
                                    Integer selfServiceUnitPriceFen,
                                    Integer tablewareCount,
                                    Integer tablewareUnitPriceFen) {
        SessionCheckout existing = checkoutMapper.selectOne(
                new LambdaQueryWrapper<SessionCheckout>().eq(SessionCheckout::getSessionId, sessionId)
        );
        if (existing != null) {
            return existing;
        }

        DiningSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() != 0) {
            throw new IllegalArgumentException("Session does not exist or is already closed");
        }

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, sessionId)
                        .notIn(Orders::getStatus, 4, 5)
        );
        for (Orders order : orders) {
            if (Objects.equals(order.getStatus(), 0)) {
                throw new IllegalArgumentException("There are unconfirmed orders in this session");
            }
        }

        List<Integer> orderIds = orders.stream()
                .map(Orders::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<OrderItem> allItems = orderIds.isEmpty() ? Collections.emptyList() :
                orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId, orderIds)
                                .in(OrderItem::getStatus, 1, 2)
                );

        int dishAmount = 0;
        for (OrderItem item : allItems) {
            dishAmount += item.getDishPrice() * item.getQuantity();
        }

        int normalizedSelfServiceCount = normalizeNonNegative(selfServiceCount, "Self-service count cannot be negative");
        int normalizedTablewareCount = normalizeNonNegative(tablewareCount, "Tableware count cannot be negative");
        int normalizedSelfServiceUnitPrice = normalizeNonNegative(
                selfServiceUnitPriceFen != null ? selfServiceUnitPriceFen : defaultSelfServiceUnitPriceFen,
                "Self-service unit price cannot be negative"
        );
        int normalizedTablewareUnitPrice = normalizeNonNegative(
                tablewareUnitPriceFen != null ? tablewareUnitPriceFen : defaultTablewareUnitPriceFen,
                "Tableware unit price cannot be negative"
        );
        int selfServiceAmount = normalizedSelfServiceCount * normalizedSelfServiceUnitPrice;
        int tablewareAmount = normalizedTablewareCount * normalizedTablewareUnitPrice;
        int totalAmount = dishAmount + selfServiceAmount + tablewareAmount;

        if (actualPaidFen == null) {
            throw new IllegalArgumentException("Actual paid amount is required");
        }
        if (actualPaidFen < 0) {
            throw new IllegalArgumentException("Actual paid amount cannot be negative");
        }
        if (actualPaidFen > totalAmount) {
            throw new IllegalArgumentException("Actual paid amount cannot exceed total amount");
        }

        int discountAmount = totalAmount - actualPaidFen;
        int pointsEarned = actualPaidFen / 100;

        SessionCheckout checkout = new SessionCheckout();
        checkout.setSessionId(sessionId);
        checkout.setTotalAmount(totalAmount);
        checkout.setDishAmount(dishAmount);
        checkout.setActualPaid(actualPaidFen);
        checkout.setDiscountAmount(discountAmount);
        checkout.setSelfServiceCount(normalizedSelfServiceCount);
        checkout.setSelfServiceUnitPrice(normalizedSelfServiceUnitPrice);
        checkout.setSelfServiceAmount(selfServiceAmount);
        checkout.setTablewareCount(normalizedTablewareCount);
        checkout.setTablewareUnitPrice(normalizedTablewareUnitPrice);
        checkout.setTablewareAmount(tablewareAmount);
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
            pointsRecord.setRemark("earned from checkout");
            pointsRecordMapper.insert(pointsRecord);

            int affected = customerMapper.adjustBalances(customerId, pointsEarned, actualPaidFen);
            if (affected == 0) {
                throw new IllegalArgumentException("Customer does not exist");
            }

            ordersMapper.updateCustomerBySessionId(sessionId, customerId);
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

    private TableInfo requireTable(Integer tableId) {
        if (tableId == null) {
            throw new IllegalArgumentException("Table id is required");
        }
        TableInfo table = tableInfoMapper.selectById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("Table does not exist");
        }
        return table;
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
            wrapper.ge(SessionCheckout::getCheckoutTime, parseDateOrThrow(startDate, "Invalid start date").atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(SessionCheckout::getCheckoutTime, parseDateOrThrow(endDate, "Invalid end date").atTime(23, 59, 59));
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
            sessionMap = sessions.stream().collect(Collectors.toMap(DiningSession::getId, session -> session));

            Set<Integer> tableIds = sessions.stream()
                    .map(DiningSession::getTableId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!tableIds.isEmpty()) {
                tableMap = tableInfoMapper.selectList(
                        new LambdaQueryWrapper<TableInfo>().in(TableInfo::getId, tableIds)
                ).stream().collect(Collectors.toMap(TableInfo::getId, table -> table));
            }
        }

        List<CheckoutHistoryDTO> result = new ArrayList<>();
        for (SessionCheckout checkout : checkouts) {
            CheckoutHistoryDTO dto = new CheckoutHistoryDTO();
            dto.setId(checkout.getId());
            dto.setSessionId(checkout.getSessionId());
            dto.setTotalAmount(checkout.getTotalAmount());
            dto.setDishAmount(resolveDishAmount(checkout));
            dto.setActualPaid(checkout.getActualPaid());
            dto.setDiscountAmount(checkout.getDiscountAmount());
            dto.setSelfServiceCount(valueOrZero(checkout.getSelfServiceCount()));
            dto.setSelfServiceUnitPrice(valueOrZero(checkout.getSelfServiceUnitPrice()));
            dto.setSelfServiceAmount(valueOrZero(checkout.getSelfServiceAmount()));
            dto.setTablewareCount(valueOrZero(checkout.getTablewareCount()));
            dto.setTablewareUnitPrice(valueOrZero(checkout.getTablewareUnitPrice()));
            dto.setTablewareAmount(valueOrZero(checkout.getTablewareAmount()));
            dto.setPointsEarned(checkout.getPointsEarned());
            dto.setCheckoutTime(checkout.getCheckoutTime());

            DiningSession session = sessionMap.get(checkout.getSessionId());
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

    public List<CustomerConsumptionDTO> getCustomerConsumptionRecords(Integer customerId) {
        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .eq(SessionCheckout::getCustomerId, customerId)
                        .orderByDesc(SessionCheckout::getCheckoutTime)
        );
        if (checkouts.isEmpty()) {
            return Collections.emptyList();
        }

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
            sessionMap = sessions.stream()
                    .collect(Collectors.toMap(DiningSession::getId, session -> session));

            Set<Integer> tableIds = sessions.stream()
                    .map(DiningSession::getTableId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!tableIds.isEmpty()) {
                tableMap = tableInfoMapper.selectList(
                        new LambdaQueryWrapper<TableInfo>().in(TableInfo::getId, tableIds)
                ).stream().collect(Collectors.toMap(TableInfo::getId, table -> table));
            }
        }

        List<Orders> orders = sessionIds.isEmpty()
                ? Collections.emptyList()
                : ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .in(Orders::getSessionId, sessionIds)
                        .eq(Orders::getCustomerId, customerId)
                        .orderByAsc(Orders::getCreateTime)
        );
        Map<Integer, List<Orders>> ordersBySessionId = orders.stream()
                .collect(Collectors.groupingBy(Orders::getSessionId, LinkedHashMap::new, Collectors.toList()));

        List<Integer> orderIds = orders.stream()
                .map(Orders::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<OrderItem> orderItems = orderIds.isEmpty()
                ? Collections.emptyList()
                : orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .in(OrderItem::getStatus, 1, 2)
        );
        Map<Integer, List<OrderItem>> itemsByOrderId = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<CustomerConsumptionDTO> records = new ArrayList<>();
        for (SessionCheckout checkout : checkouts) {
            CustomerConsumptionDTO dto = new CustomerConsumptionDTO();
            dto.setCheckoutId(checkout.getId());
            dto.setSessionId(checkout.getSessionId());
            dto.setTotalAmount(checkout.getTotalAmount());
            dto.setDishAmount(resolveDishAmount(checkout));
            dto.setActualPaid(checkout.getActualPaid());
            dto.setDiscountAmount(checkout.getDiscountAmount());
            dto.setSelfServiceCount(valueOrZero(checkout.getSelfServiceCount()));
            dto.setSelfServiceUnitPrice(valueOrZero(checkout.getSelfServiceUnitPrice()));
            dto.setSelfServiceAmount(valueOrZero(checkout.getSelfServiceAmount()));
            dto.setTablewareCount(valueOrZero(checkout.getTablewareCount()));
            dto.setTablewareUnitPrice(valueOrZero(checkout.getTablewareUnitPrice()));
            dto.setTablewareAmount(valueOrZero(checkout.getTablewareAmount()));
            dto.setPointsEarned(checkout.getPointsEarned());
            dto.setCheckoutTime(checkout.getCheckoutTime());

            DiningSession session = sessionMap.get(checkout.getSessionId());
            if (session != null && session.getTableId() != null) {
                TableInfo table = tableMap.get(session.getTableId());
                if (table != null) {
                    dto.setTableName(table.getName());
                    dto.setTableArea(table.getArea());
                }
            }

            List<Orders> sessionOrders = new ArrayList<>(ordersBySessionId.getOrDefault(checkout.getSessionId(), Collections.emptyList()));
            for (Orders order : sessionOrders) {
                order.setItems(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()));
            }
            dto.setOrders(sessionOrders);
            dto.setOrderCount(sessionOrders.size());

            List<OrderItem> consumedItems = sessionOrders.stream()
                    .flatMap(order -> order.getItems().stream())
                    .collect(Collectors.toList());
            dto.setDishSummary(buildDishSummary(consumedItems));
            records.add(dto);
        }
        return records;
    }

    private LocalDate parseDateOrThrow(String dateStr, String errorMsg) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private List<DishSummaryDTO> buildDishSummary(List<OrderItem> items) {
        Map<String, DishSummaryDTO> summaryByDish = new LinkedHashMap<>();
        for (OrderItem item : items) {
            String portionType = PortionType.normalizeStored(item.getPortionType());
            String summaryKey = item.getDishId() + "|" + portionType + "|" + item.getDishPrice() + "|" + item.getDishName();
            DishSummaryDTO summary = summaryByDish.computeIfAbsent(summaryKey, key -> {
                DishSummaryDTO dto = new DishSummaryDTO();
                dto.setDishId(item.getDishId());
                dto.setDishName(item.getDishName());
                dto.setDishPrice(item.getDishPrice());
                dto.setPortionType(portionType);
                dto.setQuantity(0);
                dto.setAmount(0);
                return dto;
            });
            summary.setQuantity(summary.getQuantity() + item.getQuantity());
            summary.setAmount(summary.getAmount() + item.getDishPrice() * item.getQuantity());
        }
        return new ArrayList<>(summaryByDish.values());
    }

    private int safeNonNegative(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private int normalizeNonNegative(Integer value, String errorMessage) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int resolveDishAmount(SessionCheckout checkout) {
        if (checkout == null) {
            return 0;
        }
        Integer dishAmount = checkout.getDishAmount();
        if (dishAmount != null) {
            return dishAmount;
        }
        return valueOrZero(checkout.getTotalAmount());
    }
}
